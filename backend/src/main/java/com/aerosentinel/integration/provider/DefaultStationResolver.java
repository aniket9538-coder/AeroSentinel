package com.aerosentinel.integration.provider;

import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DefaultStationResolver implements StationResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultStationResolver.class);
    private static final double PROXIMITY_THRESHOLD_KM = 0.5; // 500 meters tolerance
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final SensorRepository sensorRepository;

    // Explicit deterministic mapping for known external provider location IDs to AeroSentinel station codes
    private final Map<String, String> providerStationMapping = new HashMap<>();

    public DefaultStationResolver(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
        initializeDefaultMappings();
    }

    private void initializeDefaultMappings() {
        // OpenAQ location IDs for Pune stations
        providerStationMapping.put("8118", "PUN-001");
        providerStationMapping.put("8119", "PUN-002");
        providerStationMapping.put("8120", "PUN-003");
        providerStationMapping.put("11613", "PUN-001");   // Revenue Colony-Shivajinagar, Pune - IITM
        providerStationMapping.put("3409438", "PUN-002"); // Katraj Dairy, Pune - MPCB
        providerStationMapping.put("60658", "PUN-003");   // Hadapsar, Pune - IITM

        // OpenAQ location IDs for Mumbai stations
        providerStationMapping.put("6945", "MUM-001");   // Kurla, Mumbai - MPCB
        providerStationMapping.put("6948", "MUM-002");   // Chhatrapati Shivaji Intl. Airport (T2), Mumbai - MPCB

        // OpenAQ location IDs for Delhi stations
        providerStationMapping.put("17", "DEL-001");     // R K Puram, Delhi - DPCC
        providerStationMapping.put("235", "DEL-002");    // Anand Vihar, New Delhi - DPCC
        providerStationMapping.put("50", "DEL-003");     // Punjabi Bagh, Delhi - DPCC

        // Identity mappings
        providerStationMapping.put("PUN-001", "PUN-001");
        providerStationMapping.put("PUN-002", "PUN-002");
        providerStationMapping.put("PUN-003", "PUN-003");
        providerStationMapping.put("MUM-001", "MUM-001");
        providerStationMapping.put("MUM-002", "MUM-002");
        providerStationMapping.put("DEL-001", "DEL-001");
        providerStationMapping.put("DEL-002", "DEL-002");
        providerStationMapping.put("DEL-003", "DEL-003");
    }

    public void registerMapping(String providerStationId, String stationCode) {
        providerStationMapping.put(providerStationId, stationCode);
    }

    @Override
    public Optional<MonitoringStation> resolveStation(ProviderObservation observation) {
        if (observation == null) {
            log.warn("Cannot resolve station: observation is null");
            return Optional.empty();
        }

        String providerStationId = observation.getProviderStationId();

        // 1. Check explicit provider station ID mapping
        if (providerStationId != null && providerStationMapping.containsKey(providerStationId)) {
            String targetStationCode = providerStationMapping.get(providerStationId);
            Optional<MonitoringStation> stationOpt = sensorRepository.findByStationCode(targetStationCode);
            if (stationOpt.isPresent()) {
                log.debug("Resolved provider station '{}' to internal station '{}' via explicit mapping",
                        providerStationId, targetStationCode);
                return stationOpt;
            } else {
                log.warn("Provider station '{}' mapped to '{}', but station not found in database",
                        providerStationId, targetStationCode);
            }
        }

        // 2. Direct station code lookup in database
        if (providerStationId != null) {
            Optional<MonitoringStation> directOpt = sensorRepository.findByStationCode(providerStationId);
            if (directOpt.isPresent()) {
                log.debug("Resolved provider station '{}' directly to internal station in database", providerStationId);
                return directOpt;
            }
        }

        // 3. Deterministic coordinate proximity matching against active stations
        if (observation.getLatitude() != null && observation.getLongitude() != null) {
            List<MonitoringStation> allStations = sensorRepository.findAll();
            MonitoringStation bestMatch = null;
            double minDistance = Double.MAX_VALUE;

            for (MonitoringStation station : allStations) {
                if ("ACTIVE".equalsIgnoreCase(station.getStatus()) &&
                        station.getLatitude() != null && station.getLongitude() != null) {
                    double dist = calculateDistanceKm(
                            observation.getLatitude(), observation.getLongitude(),
                            station.getLatitude(), station.getLongitude());
                    if (dist <= PROXIMITY_THRESHOLD_KM && dist < minDistance) {
                        minDistance = dist;
                        bestMatch = station;
                    }
                }
            }

            if (bestMatch != null) {
                log.debug("Resolved provider coordinates ({}, {}) to internal station '{}' (distance: {:.3f} km)",
                        observation.getLatitude(), observation.getLongitude(), bestMatch.getStationCode(), minDistance);
                return Optional.of(bestMatch);
            }
        }

        log.warn("Station resolution failed for provider station '{}' at coordinates ({}, {})",
                providerStationId, observation.getLatitude(), observation.getLongitude());
        return Optional.empty();
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
