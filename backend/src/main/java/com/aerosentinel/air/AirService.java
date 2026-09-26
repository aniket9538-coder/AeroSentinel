package com.aerosentinel.air;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.air.AirQualityHistoryResponse;
import com.aerosentinel.dto.air.AirQualityObservationResponse;
import com.aerosentinel.dto.air.HistoricalObservationResponse;
import com.aerosentinel.dto.air.LatestAirQualityResponse;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AirService {

    private final AirObservationRepository airObservationRepository;
    private final CityRepository cityRepository;
    private final SensorRepository sensorRepository;

    public AirService(AirObservationRepository airObservationRepository,
                      CityRepository cityRepository,
                      SensorRepository sensorRepository) {
        this.airObservationRepository = airObservationRepository;
        this.cityRepository = cityRepository;
        this.sensorRepository = sensorRepository;
    }

    /**
     * F1 Canonical Endpoint Service: Latest air quality for a city.
     * Returns latest available PM2.5 observation for each active monitoring station in the city.
     */
    public LatestAirQualityResponse getLatestAirQualityForCity(UUID cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found with id: " + cityId));

        List<MonitoringStation> stations = sensorRepository.findByCityIdAndStatus(cityId, "ACTIVE");
        if (stations.isEmpty()) {
            stations = sensorRepository.findByCityId(cityId);
        }

        List<AirQualityObservationResponse> observations = new ArrayList<>();
        for (MonitoringStation station : stations) {
            Optional<AirObservation> latestOpt = airObservationRepository
                    .findFirstByStationIdOrderByObservedAtDesc(station.getStationCode());
            latestOpt.ifPresent(obs -> observations.add(new AirQualityObservationResponse(
                    station.getStationCode(),
                    station.getName(),
                    obs.getPm25(),
                    obs.getObservedAt(),
                    obs.getSource(),
                    obs.getDataQuality() != null ? obs.getDataQuality() : "VALID"
            )));
        }

        return new LatestAirQualityResponse(city.getId(), city.getName(), observations);
    }

    /**
     * F1 Canonical Endpoint Service: Station historical air quality observations within time range.
     */
    public AirQualityHistoryResponse getStationAirQualityHistory(String stationId, String fromStr, String toStr) {
        if (fromStr == null || fromStr.isBlank()) {
            throw new ValidationException("Parameter 'from' is required");
        }
        if (toStr == null || toStr.isBlank()) {
            throw new ValidationException("Parameter 'to' is required");
        }

        Instant from;
        try {
            from = Instant.parse(fromStr.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid 'from' timestamp format: '" + fromStr + "'. Expected ISO-8601 UTC format (e.g. 2026-09-24T00:00:00Z)");
        }

        Instant to;
        try {
            to = Instant.parse(toStr.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid 'to' timestamp format: '" + toStr + "'. Expected ISO-8601 UTC format (e.g. 2026-09-24T23:59:59Z)");
        }

        return getStationAirQualityHistory(stationId, from, to);
    }

    /**
     * Overloaded helper with typed timestamps.
     */
    public AirQualityHistoryResponse getStationAirQualityHistory(String stationId, Instant from, Instant to) {
        if (from == null) {
            throw new ValidationException("Parameter 'from' is required");
        }
        if (to == null) {
            throw new ValidationException("Parameter 'to' is required");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("Invalid date range: 'from' (" + from + ") must not be after 'to' (" + to + ")");
        }

        MonitoringStation station = sensorRepository.findByStationCode(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitoring station not found with code: " + stationId));

        List<AirObservation> observations = airObservationRepository
                .findByStationIdAndObservedAtBetweenOrderByObservedAtAsc(stationId, from, to);

        List<HistoricalObservationResponse> observationDtos = observations.stream()
                .map(obs -> new HistoricalObservationResponse(
                        obs.getPm25(),
                        obs.getObservedAt(),
                        obs.getSource(),
                        obs.getDataQuality() != null ? obs.getDataQuality() : "VALID"
                ))
                .collect(Collectors.toList());

        return new AirQualityHistoryResponse(station.getStationCode(), station.getName(), observationDtos);
    }

    // Existing legacy methods preserved for backward compatibility
    public List<AirObservation> getCurrentAirObservations(UUID cityId) {
        return airObservationRepository.findByCityIdOrderByObservedAtDesc(cityId);
    }

    public List<AirObservation> getStationObservations(String stationId) {
        return airObservationRepository.findByStationIdOrderByObservedAtDesc(stationId);
    }

    public AirObservation saveObservation(AirObservation observation) {
        return airObservationRepository.save(observation);
    }
}
