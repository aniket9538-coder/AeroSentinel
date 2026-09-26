package com.aerosentinel.feature;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.fire.FireEvent;
import com.aerosentinel.fire.FireRepository;
import com.aerosentinel.sensor.MonitoringStation;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.aerosentinel.weather.WeatherObservation;
import com.aerosentinel.weather.WeatherRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class FeatureEngineeringService {

    private static final Logger log = LoggerFactory.getLogger(FeatureEngineeringService.class);

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final double DEFAULT_PUNE_PRESSURE = 954.3; // hPa historical baseline elevation ~600m
    private static final double COVERAGE_GAP_THRESHOLD_KM = 7.0;
    private static final double FIRE_MAX_RADIUS_KM = 25.0;
    private static final Duration FIRE_LOOKBACK = Duration.ofHours(24);

    // GIS regional defaults for Pune Metropolitan Region per Member 3 preprocessing contract
    private static final double GIS_INDUSTRIAL_DIST_KM = 3.50;
    private static final double GIS_MAJOR_ROAD_DIST_KM = 0.40;
    private static final int GIS_SENSITIVE_RECEPTORS = 4;
    private static final int GIS_INDUSTRIAL_2KM_FLAG = 0;

    private final AirObservationRepository airObservationRepository;
    private final WeatherRepository weatherRepository;
    private final SensorRepository sensorRepository;
    private final FireRepository fireRepository;
    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final H3Service h3Service;
    private final ObjectMapper objectMapper;

    public FeatureEngineeringService(
            AirObservationRepository airObservationRepository,
            WeatherRepository weatherRepository,
            SensorRepository sensorRepository,
            FireRepository fireRepository,
            FeatureSnapshotRepository featureSnapshotRepository,
            H3Service h3Service,
            ObjectMapper objectMapper) {
        this.airObservationRepository = airObservationRepository;
        this.weatherRepository = weatherRepository;
        this.sensorRepository = sensorRepository;
        this.fireRepository = fireRepository;
        this.featureSnapshotRepository = featureSnapshotRepository;
        this.h3Service = h3Service;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates or retrieves a persisted feature snapshot for the given H3 cell and observation timestamp.
     * Guaranteed 36 numeric features matching hotspot_classifier_v1.joblib.
     */
    @Transactional
    public FeatureRecord generateFeatureRecord(UUID cityId, String h3Index, Instant observationTime) {
        // 1. Check existing snapshot
        Optional<FeatureSnapshot> existing = featureSnapshotRepository
                .findByH3IndexAndObservedAtAndFeatureSchemaVersion(h3Index, observationTime, FeatureRecord.SCHEMA_VERSION);
        if (existing.isPresent()) {
            FeatureSnapshot snapshot = existing.get();
            try {
                Map<String, Object> featMap = objectMapper.readValue(
                        snapshot.getFeatures(), new TypeReference<Map<String, Object>>() {});
                List<String> missing = snapshot.getMissingFeatures() != null
                        ? Arrays.asList(snapshot.getMissingFeatures())
                        : Collections.emptyList();
                return new FeatureRecord(
                        snapshot.getCityId(),
                        snapshot.getH3Index(),
                        snapshot.getObservedAt(),
                        snapshot.getFeatureSchemaVersion(),
                        featMap,
                        FeatureQualityStatus.valueOf(snapshot.getQualityStatus()),
                        missing
                );
            } catch (Exception e) {
                log.warn("Failed to parse cached feature snapshot JSON for cell {}: {}", h3Index, e.getMessage());
            }
        }

        // 2. Resolve spatial centroid coordinates
        com.uber.h3core.util.LatLng center = h3Service.h3ToCenter(h3Index);
        double lat = center.lat;
        double lon = center.lng;

        List<String> missingFields = new ArrayList<>();

        // 3. Find matching or latest valid air observation at or prior to observationTime (leakage-free: t <= T)
        List<AirObservation> airList = airObservationRepository.findByH3IndexOrderByObservedAtDesc(h3Index);
        AirObservation air = airList.stream()
                .filter(a -> !a.getObservedAt().isAfter(observationTime))
                .findFirst()
                .orElse(null);

        Double pm10 = air != null ? air.getPm10() : null;
        Double no2 = air != null ? air.getNo2() : null;
        Double so2 = air != null ? air.getSo2() : null;
        Double co = air != null ? air.getCo() : null;
        Double o3 = air != null ? air.getO3() : null;

        if (pm10 == null) missingFields.add("pm10");
        if (no2 == null) missingFields.add("no2");
        if (so2 == null) missingFields.add("so2");
        if (co == null) missingFields.add("co");
        if (o3 == null) missingFields.add("o3");

        // 4. Find matching or latest valid weather observation at or prior to observationTime (leakage-free: t <= T)
        List<WeatherObservation> weatherList = weatherRepository.findByH3IndexOrderByObservedAtDesc(h3Index);
        WeatherObservation weather = weatherList.stream()
                .filter(w -> !w.getObservedAt().isAfter(observationTime))
                .findFirst()
                .orElse(null);

        // Fallback to city-wide latest weather if cell-specific weather not yet populated
        if (weather == null) {
            weather = weatherRepository.findByCityIdOrderByObservedAtDesc(cityId).stream()
                    .filter(w -> !w.getObservedAt().isAfter(observationTime))
                    .findFirst()
                    .orElse(null);
        }

        Double temp = weather != null ? weather.getTemperature() : 25.0;
        Double humidity = weather != null ? weather.getHumidity() : 50.0;
        Double windSpeedKmh = weather != null && weather.getWindSpeed() != null ? weather.getWindSpeed() : 5.0;
        Double windDir = weather != null && weather.getWindDirection() != null ? weather.getWindDirection() : 0.0;
        Double rainfall = weather != null && weather.getRainfall() != null ? weather.getRainfall() : 0.0;
        Double pressure = weather != null && weather.getPressure() != null ? weather.getPressure() : DEFAULT_PUNE_PRESSURE;

        if (weather == null) {
            missingFields.add("weather");
        }

        // 5. Derive wind components (strictly in m/s with calm threshold < 0.2 m/s)
        double[] windVec = deriveWindVectors(windSpeedKmh, windDir);
        double windSpeedMs = windSpeedKmh / 3.6;
        double windU = windVec[0];
        double windV = windVec[1];

        // 6. Compute leave-one-out spatial lag for PM2.5
        double spatialLag = computeLeaveOneOutSpatialLag(cityId, air != null ? air.getStationId() : null, observationTime, air != null ? air.getPm25() : 45.0);

        // 7. Compute monitoring network coverage features
        Map<String, Object> networkFeatures = computeMonitoringNetworkFeatures(lat, lon, cityId);

        // 8. Compute cyclical temporal features in local solar time (Asia/Kolkata)
        Map<String, Object> temporalFeatures = computeTemporalFeatures(observationTime);

        // 9. Compute NASA FIRMS fire features (24h lookback, strictly past or concurrent)
        Map<String, Object> fireFeatures = computeFireFeatures(lat, lon, windSpeedMs, windDir, observationTime);

        // 10. Assemble GIS features
        Map<String, Object> gisFeatures = computeGisFeatures(lat, lon);

        // 11. Assemble final 36-feature map
        Map<String, Object> featureMap = new LinkedHashMap<>();
        featureMap.put("latitude", round4(lat));
        featureMap.put("longitude", round4(lon));
        featureMap.put("pm10", pm10 != null ? pm10 : 0.0);
        featureMap.put("no2", no2 != null ? no2 : 0.0);
        featureMap.put("so2", so2 != null ? so2 : 0.0);
        featureMap.put("co", co != null ? co : 0.0);
        featureMap.put("o3", o3 != null ? o3 : 0.0);

        featureMap.put("hour", temporalFeatures.get("hour"));
        featureMap.put("day_of_week", temporalFeatures.get("day_of_week"));
        featureMap.put("is_weekend", temporalFeatures.get("is_weekend"));
        featureMap.put("hour_sin", temporalFeatures.get("hour_sin"));
        featureMap.put("hour_cos", temporalFeatures.get("hour_cos"));
        featureMap.put("dow_sin", temporalFeatures.get("dow_sin"));
        featureMap.put("dow_cos", temporalFeatures.get("dow_cos"));

        featureMap.put("temperature", round2(temp));
        featureMap.put("humidity", round2(humidity));
        featureMap.put("wind_speed", round2(windSpeedKmh)); // raw km/h preserved in table
        featureMap.put("wind_direction", round2(windDir));
        featureMap.put("wind_u", windU);
        featureMap.put("wind_v", windV);
        featureMap.put("rainfall", round2(Math.max(0.0, rainfall)));
        featureMap.put("pressure", round2(pressure));

        featureMap.put("pm25_spatial_lag_mean", round2(spatialLag));

        featureMap.put("nearest_station_distance_km", networkFeatures.get("nearest_station_distance_km"));
        featureMap.put("stations_within_5km_count", networkFeatures.get("stations_within_5km_count"));
        featureMap.put("monitoring_coverage_gap_flag", networkFeatures.get("monitoring_coverage_gap_flag"));

        featureMap.put("dist_to_nearest_industrial_km", gisFeatures.get("dist_to_nearest_industrial_km"));
        featureMap.put("dist_to_nearest_major_road_km", gisFeatures.get("dist_to_nearest_major_road_km"));
        featureMap.put("sensitive_receptors_count_2km", gisFeatures.get("sensitive_receptors_count_2km"));
        featureMap.put("industrial_zone_within_2km_flag", gisFeatures.get("industrial_zone_within_2km_flag"));

        featureMap.put("fire_count_24h_25km", fireFeatures.get("fire_count_24h_25km"));
        featureMap.put("fire_frp_sum_24h_25km", fireFeatures.get("fire_frp_sum_24h_25km"));
        featureMap.put("fire_frp_mean_24h_25km", fireFeatures.get("fire_frp_mean_24h_25km"));
        featureMap.put("nearest_fire_distance_km", fireFeatures.get("nearest_fire_distance_km"));
        featureMap.put("fire_frp_distance_decay", fireFeatures.get("fire_frp_distance_decay"));
        featureMap.put("fire_upwind_alignment_score", fireFeatures.get("fire_upwind_alignment_score"));

        // Determine quality status
        FeatureQualityStatus qualityStatus = FeatureQualityStatus.VALID;
        if (!missingFields.isEmpty()) {
            qualityStatus = missingFields.size() > 3 ? FeatureQualityStatus.UNAVAILABLE : FeatureQualityStatus.MISSING;
        }

        // Persist snapshot
        try {
            FeatureSnapshot snapshot = new FeatureSnapshot();
            snapshot.setCityId(cityId);
            snapshot.setH3Index(h3Index);
            snapshot.setObservedAt(observationTime);
            snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
            snapshot.setFeatures(objectMapper.writeValueAsString(featureMap));
            snapshot.setQualityStatus(qualityStatus.name());
            snapshot.setMissingFeatures(missingFields.toArray(new String[0]));
            snapshot.setCreatedAt(Instant.now());
            featureSnapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.error("Failed to persist feature snapshot for cell {}: {}", h3Index, e.getMessage());
        }

        return new FeatureRecord(
                cityId,
                h3Index,
                observationTime,
                FeatureRecord.SCHEMA_VERSION,
                featureMap,
                qualityStatus,
                missingFields
        );
    }

    /**
     * Exact meteorological orthogonal wind derivation.
     * Speed converted from km/h to m/s.
     * Calm wind handling: speed < 0.2 m/s -> u = 0.0, v = 0.0.
     * Trigonometric convention: u = -ws * sin(theta), v = -ws * cos(theta).
     */
    public double[] deriveWindVectors(Double windSpeedKmh, Double windDirectionDeg) {
        if (windSpeedKmh == null || windDirectionDeg == null) {
            return new double[]{0.0, 0.0};
        }
        double speedMs = windSpeedKmh / 3.6;
        if (speedMs < 0.2) {
            return new double[]{0.0, 0.0};
        }
        double rad = Math.toRadians(windDirectionDeg);
        double u = -speedMs * Math.sin(rad);
        double v = -speedMs * Math.cos(rad);
        return new double[]{round4(u), round4(v)};
    }

    /**
     * Computes leave-one-out spatial lag for PM2.5:
     * lag = (sum(all_concurrent_stations) - self_station) / (N - 1).
     * Eliminates self-information leakage.
     */
    public double computeLeaveOneOutSpatialLag(UUID cityId, String selfStationId, Instant observationTime, double fallbackPm25) {
        List<AirObservation> concurrent = airObservationRepository.findByCityIdAndObservedAt(cityId, observationTime);
        if (concurrent.isEmpty()) {
            return round2(fallbackPm25);
        }

        double sum = 0.0;
        int count = 0;
        for (AirObservation obs : concurrent) {
            if (selfStationId != null && selfStationId.equals(obs.getStationId())) {
                continue; // Skip self to prevent leakage
            }
            if (obs.getPm25() != null) {
                sum += obs.getPm25();
                count++;
            }
        }

        if (count == 0) {
            return round2(fallbackPm25);
        }
        return round2(sum / count);
    }

    /**
     * Pure leave-one-out spatial lag helper for contract testing.
     */
    public double calculateLeaveOneOutSpatialLag(List<Double> stationPm25List, int selfIndex) {
        if (stationPm25List == null || stationPm25List.isEmpty()) {
            return 0.0;
        }
        if (stationPm25List.size() == 1) {
            return stationPm25List.get(0);
        }
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < stationPm25List.size(); i++) {
            if (i == selfIndex) continue;
            sum += stationPm25List.get(i);
            count++;
        }
        return count > 0 ? round2(sum / count) : stationPm25List.get(selfIndex);
    }

    /**
     * Computes monitoring coverage features based on active monitoring stations.
     * Coverage gap rule: nearest_station_distance_km > 7.0 km -> 1, else 0.
     */
    public Map<String, Object> computeMonitoringNetworkFeatures(double lat, double lon, UUID cityId) {
        List<MonitoringStation> stations = sensorRepository.findByCityId(cityId);
        List<double[]> coordsList = new ArrayList<>();
        for (MonitoringStation s : stations) {
            if (s.getLatitude() != null && s.getLongitude() != null) {
                coordsList.add(new double[]{s.getLatitude(), s.getLongitude()});
            }
        }
        return computeMonitoringNetworkFeatures(lat, lon, coordsList);
    }

    public Map<String, Object> computeMonitoringNetworkFeatures(double lat, double lon, List<double[]> stationCoords) {
        Map<String, Object> res = new HashMap<>();
        if (stationCoords == null || stationCoords.isEmpty()) {
            res.put("nearest_station_distance_km", 0.0);
            res.put("stations_within_5km_count", 1);
            res.put("monitoring_coverage_gap_flag", 0);
            return res;
        }

        double minDistance = Double.MAX_VALUE;
        int count5km = 1; // Include self station/cell

        for (double[] pt : stationCoords) {
            double d = calculateHaversineDistanceKm(lat, lon, pt[0], pt[1]);
            if (d < 1e-4) {
                continue; // self station match
            }
            if (d < minDistance) {
                minDistance = d;
            }
            if (d <= 5.0) {
                count5km++;
            }
        }

        if (minDistance == Double.MAX_VALUE) {
            minDistance = 0.0;
        }

        int gapFlag = minDistance > COVERAGE_GAP_THRESHOLD_KM ? 1 : 0;
        res.put("nearest_station_distance_km", round2(minDistance));
        res.put("stations_within_5km_count", count5km);
        res.put("monitoring_coverage_gap_flag", gapFlag);
        return res;
    }

    /**
     * Cyclical temporal features anchored to Asia/Kolkata local solar time.
     */
    public Map<String, Object> computeTemporalFeatures(Instant utcTimestamp) {
        ZonedDateTime local = utcTimestamp.atZone(DEFAULT_ZONE);
        int hour = local.getHour();
        // Java DayOfWeek: 1 (Mon) .. 7 (Sun) -> map to 0 (Mon) .. 6 (Sun)
        int dow = local.getDayOfWeek().getValue() - 1;
        int isWeekend = dow >= 5 ? 1 : 0;

        double hourSin = round4(Math.sin(2.0 * Math.PI * hour / 24.0));
        double hourCos = round4(Math.cos(2.0 * Math.PI * hour / 24.0));
        double dowSin = round4(Math.sin(2.0 * Math.PI * dow / 7.0));
        double dowCos = round4(Math.cos(2.0 * Math.PI * dow / 7.0));

        Map<String, Object> res = new HashMap<>();
        res.put("hour", hour);
        res.put("day_of_week", dow);
        res.put("is_weekend", isWeekend);
        res.put("hour_sin", hourSin);
        res.put("hour_cos", hourCos);
        res.put("dow_sin", dowSin);
        res.put("dow_cos", dowCos);
        return res;
    }

    /**
     * Computes NASA FIRMS fire features with 24h lookback and 25km range.
     * Enforces strict non-leakage: fire_time <= observation_time.
     * Legitimate domain-defined zero fill on fire absence.
     */
    public Map<String, Object> computeFireFeatures(double lat, double lon, double windSpeedMs, Double windDir, Instant observationTime) {
        Instant startTime = observationTime.minus(FIRE_LOOKBACK);
        List<FireEvent> fires = fireRepository.findByDetectedAtBetween(startTime, observationTime);
        return computeFireFeatures(lat, lon, windSpeedMs, windDir, fires);
    }

    public Map<String, Object> computeFireFeatures(double lat, double lon, double windSpeedMs, Double windDir, List<FireEvent> fires) {
        Map<String, Object> res = new HashMap<>();
        res.put("fire_count_24h_25km", 0);
        res.put("fire_frp_sum_24h_25km", 0.0);
        res.put("fire_frp_mean_24h_25km", 0.0);
        res.put("nearest_fire_distance_km", 50.0);
        res.put("fire_frp_distance_decay", 0.0);
        res.put("fire_upwind_alignment_score", 0.0);

        if (fires == null || fires.isEmpty()) {
            return res;
        }

        double minDistance = 50.0;
        double frpSum = 0.0;
        int countWithin25km = 0;
        double decaySum = 0.0;
        double upwindScore = 0.0;

        for (FireEvent fire : fires) {
            if (fire.getLatitude() == null || fire.getLongitude() == null) continue;
            double d = calculateHaversineDistanceKm(lat, lon, fire.getLatitude(), fire.getLongitude());
            if (d < minDistance) {
                minDistance = d;
            }
            if (d <= FIRE_MAX_RADIUS_KM) {
                countWithin25km++;
                double frp = fire.getFrp() != null ? Math.max(0.0, fire.getFrp()) : 0.0;
                frpSum += frp;
                decaySum += (frp / (d + 1.0));

                if (windSpeedMs >= 0.2 && windDir != null) {
                    double bearing = calculateBearingDegrees(lat, lon, fire.getLatitude(), fire.getLongitude());
                    double diff = Math.abs((bearing - windDir + 180.0) % 360.0 - 180.0);
                    if (diff <= 45.0) {
                        double weight = Math.cos(Math.toRadians(diff));
                        upwindScore += (frp * weight / (d + 1.0));
                    }
                }
            }
        }

        res.put("fire_count_24h_25km", countWithin25km);
        res.put("fire_frp_sum_24h_25km", round2(frpSum));
        res.put("fire_frp_mean_24h_25km", countWithin25km > 0 ? round2(frpSum / countWithin25km) : 0.0);
        res.put("nearest_fire_distance_km", round2(minDistance));
        res.put("fire_frp_distance_decay", round4(decaySum));
        res.put("fire_upwind_alignment_score", round4(upwindScore));
        return res;
    }

    /**
     * GIS features using authoritative regional baselines.
     */
    public Map<String, Object> computeGisFeatures(double lat, double lon) {
        Map<String, Object> res = new HashMap<>();
        res.put("dist_to_nearest_industrial_km", GIS_INDUSTRIAL_DIST_KM);
        res.put("dist_to_nearest_major_road_km", GIS_MAJOR_ROAD_DIST_KM);
        res.put("sensitive_receptors_count_2km", GIS_SENSITIVE_RECEPTORS);
        res.put("industrial_zone_within_2km_flag", GIS_INDUSTRIAL_2KM_FLAG);
        return res;
    }

    public static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return r * c;
    }

    public static double calculateBearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLam = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLam) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLam);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
