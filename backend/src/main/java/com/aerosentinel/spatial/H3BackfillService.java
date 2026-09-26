package com.aerosentinel.spatial;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.grid.GridRepository;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative backfill service for associating all real F1 air observations with
 * deterministic Uber H3 Resolution 8 spatial cells and persisting real grid_cells records.
 */
@Service
public class H3BackfillService {

    private static final Logger log = LoggerFactory.getLogger(H3BackfillService.class);

    private final AirObservationRepository airObservationRepository;
    private final SensorRepository sensorRepository;
    private final H3Service h3Service;
    private final GridService gridService;
    private final GridRepository gridRepository;

    public H3BackfillService(AirObservationRepository airObservationRepository,
                             SensorRepository sensorRepository,
                             H3Service h3Service,
                             GridService gridService,
                             GridRepository gridRepository) {
        this.airObservationRepository = airObservationRepository;
        this.sensorRepository = sensorRepository;
        this.h3Service = h3Service;
        this.gridService = gridService;
        this.gridRepository = gridRepository;
    }

    public record BackfillSummary(
            long totalObservations,
            int observationsUpdated,
            int gridCellsCreated,
            int uniqueH3Count,
            Map<String, String> stationToH3Map
    ) {}

    /**
     * Executes the idempotent backfill:
     * 1. Derives H3 from monitoring station coordinates (latitude, longitude).
     * 2. Populates h3_index on any air_observation where h3_index is null.
     * 3. Creates/updates grid_cells records with PostGIS polygon boundaries and centroids.
     */
    @Transactional
    public BackfillSummary backfillAirObservationsAndGridCells() {
        log.info("Starting H3 spatial backfill for air observations and grid cells...");

        long totalObs = airObservationRepository.count();
        List<MonitoringStation> stations = sensorRepository.findAll();
        Map<String, MonitoringStation> stationMap = stations.stream()
                .collect(Collectors.toMap(MonitoringStation::getStationCode, s -> s));

        Map<String, String> stationToH3 = new LinkedHashMap<>();
        for (MonitoringStation station : stations) {
            String h3 = h3Service.coordinatesToH3(station.getLatitude(), station.getLongitude());
            stationToH3.put(station.getStationCode(), h3);
            log.info("Station {} ({}) -> coordinates ({}, {}) -> H3 index {}",
                    station.getStationCode(), station.getName(),
                    station.getLatitude(), station.getLongitude(), h3);
        }

        // 1. Update air observations where h3_index is null
        List<AirObservation> unassigned = airObservationRepository.findByH3IndexIsNull();
        int updatedCount = 0;

        for (AirObservation obs : unassigned) {
            MonitoringStation station = stationMap.get(obs.getStationId());
            if (station == null) {
                log.error("Fatal: station code '{}' for observation ID '{}' not found in monitoring_stations",
                        obs.getStationId(), obs.getId());
                throw new IllegalStateException("Cannot backfill H3: unknown station code " + obs.getStationId());
            }

            // Derive H3 from the station coordinates
            String h3Index = stationToH3.get(obs.getStationId());
            obs.setH3Index(h3Index);
            updatedCount++;
        }

        if (updatedCount > 0) {
            airObservationRepository.saveAll(unassigned);
            log.info("Successfully backfilled H3 index for {} air observations", updatedCount);
        } else {
            log.info("All {} air observations already have non-null H3 index (idempotent no-op)", totalObs);
        }

        // 2. Create/update grid cells for each unique H3 index
        int gridCellsCreated = 0;
        Set<String> uniqueH3Set = new HashSet<>(stationToH3.values());

        for (MonitoringStation station : stations) {
            String h3Index = stationToH3.get(station.getStationCode());
            boolean existsBefore = gridRepository.existsByH3Index(h3Index);
            gridService.getOrCreateGridCell(h3Index, station.getCityId());
            if (!existsBefore) {
                gridCellsCreated++;
            }
        }

        log.info("H3 backfill complete: totalObs={}, updated={}, gridCellsCreated={}, uniqueH3={}",
                totalObs, updatedCount, gridCellsCreated, uniqueH3Set.size());

        return new BackfillSummary(totalObs, updatedCount, gridCellsCreated, uniqueH3Set.size(), stationToH3);
    }
}
