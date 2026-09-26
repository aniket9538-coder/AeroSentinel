package com.aerosentinel.integration.provider;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.spatial.H3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    public static final String REASON_INVALID_PM25 = "REJECTED_INVALID_PM25";
    public static final String REASON_INVALID_TIMESTAMP = "REJECTED_INVALID_TIMESTAMP";
    public static final String REASON_UNMAPPED_STATION = "REJECTED_UNMAPPED_STATION";
    public static final String REASON_MISSING_SOURCE = "REJECTED_MISSING_SOURCE";

    private final AirObservationRepository airObservationRepository;
    private final StationResolver stationResolver;
    private final H3Service h3Service;
    private final GridService gridService;

    @Autowired
    public IngestionService(AirObservationRepository airObservationRepository,
                            StationResolver stationResolver,
                            H3Service h3Service,
                            GridService gridService) {
        this.airObservationRepository = airObservationRepository;
        this.stationResolver = stationResolver;
        this.h3Service = h3Service;
        this.gridService = gridService;
    }

    public IngestionService(AirObservationRepository airObservationRepository, StationResolver stationResolver) {
        this(airObservationRepository, stationResolver, new H3Service(8), null);
    }

    @Transactional
    public IngestionSummary ingest(AirQualityProvider provider) {
        return ingest(provider, null);
    }

    @Transactional
    public IngestionSummary ingest(AirQualityProvider provider, String locationId) {
        if (provider == null) {
            IngestionSummary summary = new IngestionSummary("UNKNOWN");
            summary.setStatus(ProviderStatus.SOURCE_UNAVAILABLE);
            summary.setMessage("Provider reference is null");
            return summary;
        }

        String providerName = provider.getProviderName();
        IngestionSummary summary = new IngestionSummary(providerName);

        if (!provider.isConfigured()) {
            log.warn("Ingestion skipped for provider {}: provider is not configured", providerName);
            summary.setStatus(ProviderStatus.NOT_CONFIGURED);
            summary.setMessage("Provider is not configured (missing credentials or endpoint)");
            return summary;
        }

        ProviderFetchResult fetchResult = locationId != null && !locationId.isBlank()
                ? provider.fetchLatestObservations(locationId)
                : provider.fetchLatestObservations();

        summary.setStatus(fetchResult.getStatus());
        summary.setMessage(fetchResult.getErrorMessage());

        if (!fetchResult.isSuccessful()) {
            log.warn("Fetch from provider {} was unsuccessful with status {}: {}",
                    providerName, fetchResult.getStatus(), fetchResult.getErrorMessage());
            return summary;
        }

        List<ProviderObservation> observations = fetchResult.getObservations();
        if (observations.isEmpty()) {
            log.info("Provider {} returned 0 observations (empty response)", providerName);
            summary.setStatus(ProviderStatus.EMPTY_RESPONSE);
            return summary;
        }

        processObservationsList(providerName, observations, summary);
        return summary;
    }

    @Transactional
    public IngestionSummary ingestSensorHours(com.aerosentinel.integration.openaq.OpenAqClient client, String providerLocationId, String sensorId, Instant from, Instant to) {
        if (client == null) {
            IngestionSummary summary = new IngestionSummary("OPENAQ");
            summary.setStatus(ProviderStatus.SOURCE_UNAVAILABLE);
            summary.setMessage("OpenAqClient is null");
            return summary;
        }

        String providerName = client.getProviderName();
        IngestionSummary summary = new IngestionSummary(providerName);

        if (!client.isConfigured()) {
            summary.setStatus(ProviderStatus.NOT_CONFIGURED);
            summary.setMessage("OpenAQ provider is not configured");
            return summary;
        }

        ProviderFetchResult fetchResult = client.fetchSensorHours(providerLocationId, sensorId, from, to);
        summary.setStatus(fetchResult.getStatus());
        summary.setMessage(fetchResult.getErrorMessage());

        if (!fetchResult.isSuccessful() || fetchResult.getObservations().isEmpty()) {
            return summary;
        }

        processObservationsList(providerName, fetchResult.getObservations(), summary);
        return summary;
    }

    @Transactional
    public IngestionSummary processObservations(String providerName, List<ProviderObservation> observations) {
        IngestionSummary summary = new IngestionSummary(providerName);
        if (observations == null || observations.isEmpty()) {
            summary.setStatus(ProviderStatus.EMPTY_RESPONSE);
            return summary;
        }
        processObservationsList(providerName, observations, summary);
        return summary;
    }

    private void processObservationsList(String providerName, List<ProviderObservation> observations, IngestionSummary summary) {
        for (ProviderObservation obs : observations) {
            summary.incrementFetched();

            // 1. Validate PM2.5
            if (obs.getPm25() == null || Double.isNaN(obs.getPm25()) || Double.isInfinite(obs.getPm25()) || obs.getPm25() < 0.0) {
                log.warn("Ingestion rejected: invalid PM2.5 value={} for provider station {}", obs.getPm25(), obs.getProviderStationId());
                summary.incrementRejected(REASON_INVALID_PM25);
                continue;
            }

            // 2. Validate Timestamp
            if (obs.getObservedAt() == null) {
                log.warn("Ingestion rejected: missing timestamp for provider station {}", obs.getProviderStationId());
                summary.incrementRejected(REASON_INVALID_TIMESTAMP);
                continue;
            }

            // 3. Validate Source
            String source = obs.getSource() != null && !obs.getSource().isBlank() ? obs.getSource() : providerName;
            if (source == null || source.isBlank()) {
                log.warn("Ingestion rejected: missing source provenance");
                summary.incrementRejected(REASON_MISSING_SOURCE);
                continue;
            }

            // 4. Resolve Internal Station
            Optional<MonitoringStation> stationOpt = stationResolver.resolveStation(obs);
            if (stationOpt.isEmpty()) {
                log.warn("Ingestion rejected: unable to resolve internal station for provider station ID '{}'",
                        obs.getProviderStationId());
                summary.incrementRejected(REASON_UNMAPPED_STATION);
                continue;
            }

            MonitoringStation station = stationOpt.get();
            summary.incrementMapped();

            // 5. Idempotent Duplicate Detection
            boolean exists = airObservationRepository.existsByStationIdAndObservedAt(
                    station.getStationCode(), obs.getObservedAt());
            if (exists) {
                log.info("Duplicate detected for station '{}' at observedAt '{}'. Skipping insertion.",
                        station.getStationCode(), obs.getObservedAt());
                summary.incrementDuplicates();
                continue;
            }

            // 6. Normalize & Persist to Database
            AirObservation entity = new AirObservation();
            entity.setCityId(station.getCityId());
            entity.setStationId(station.getStationCode());
            double lat = obs.getLatitude() != null ? obs.getLatitude() : station.getLatitude();
            double lon = obs.getLongitude() != null ? obs.getLongitude() : station.getLongitude();
            entity.setLatitude(lat);
            entity.setLongitude(lon);
            entity.setObservedAt(obs.getObservedAt()); // exact provider measurement timestamp
            entity.setPm25(obs.getPm25());
            entity.setSource(source);
            entity.setDataQuality(obs.getQuality() != null ? obs.getQuality() : "VALID");
            entity.setCreatedAt(Instant.now()); // server ingestion timestamp

            // 7. Authoritative H3 calculation and Grid Cell upsert
            if (h3Service != null) {
                try {
                    String h3Index = h3Service.coordinatesToH3(lat, lon);
                    entity.setH3Index(h3Index);
                    if (gridService != null) {
                        gridService.getOrCreateGridCell(h3Index, station.getCityId());
                    }
                } catch (Exception ex) {
                    log.error("Fatal H3 calculation or grid upsert failure for station {} at ({}, {}): {}",
                            station.getStationCode(), lat, lon, ex.getMessage(), ex);
                    throw new IllegalStateException("H3 spatial indexing failed for coordinates: " + ex.getMessage(), ex);
                }
            }

            airObservationRepository.save(entity);
            summary.incrementInserted();

            log.info("Successfully ingested observation: station={}, observedAt={}, pm25={}, h3Index={}, source={}",
                    station.getStationCode(), obs.getObservedAt(), obs.getPm25(), entity.getH3Index(), source);
        }
    }
}
