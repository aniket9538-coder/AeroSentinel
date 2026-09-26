package com.aerosentinel.feature;

import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.fire.FireEvent;
import com.aerosentinel.fire.FireRepository;
import com.aerosentinel.sensor.SensorRepository;
import com.aerosentinel.spatial.H3Service;
import com.aerosentinel.weather.WeatherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class FeatureEngineeringServiceTest {

    @Mock
    private AirObservationRepository airObservationRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private FireRepository fireRepository;

    @Mock
    private FeatureSnapshotRepository featureSnapshotRepository;

    private H3Service h3Service;
    private ObjectMapper objectMapper;
    private FeatureEngineeringService featureService;

    @BeforeEach
    void setUp() {
        h3Service = new H3Service(8);
        objectMapper = new ObjectMapper();
        featureService = new FeatureEngineeringService(
                airObservationRepository,
                weatherRepository,
                sensorRepository,
                fireRepository,
                featureSnapshotRepository,
                h3Service,
                objectMapper
        );
    }

    @Test
    @DisplayName("Contract 1 & 2: Exact feature names and count equal 36")
    void testExactFeatureCountAndNames() {
        assertThat(FeatureRecord.FEATURE_COUNT).isEqualTo(36);
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES).hasSize(36);
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES).containsExactly(
                "latitude", "longitude",
                "pm10", "no2", "so2", "co", "o3",
                "hour", "day_of_week", "is_weekend",
                "hour_sin", "hour_cos", "dow_sin", "dow_cos",
                "temperature", "humidity", "wind_speed", "wind_direction", "wind_u", "wind_v", "rainfall", "pressure",
                "pm25_spatial_lag_mean",
                "nearest_station_distance_km", "stations_within_5km_count", "monitoring_coverage_gap_flag",
                "dist_to_nearest_industrial_km", "dist_to_nearest_major_road_km", "sensitive_receptors_count_2km", "industrial_zone_within_2km_flag",
                "fire_count_24h_25km", "fire_frp_sum_24h_25km", "fire_frp_mean_24h_25km", "nearest_fire_distance_km", "fire_frp_distance_decay", "fire_upwind_alignment_score"
        );
    }

    @Test
    @DisplayName("Contract 6 & 7: Wind speed conversion and vector decomposition")
    void testWindConversionAndVectorDecomposition() {
        // 36 km/h = 10 m/s; wind from East (90 deg) -> blowing West (u = -10, v = 0)
        double[] vec = featureService.deriveWindVectors(36.0, 90.0);
        assertThat(vec[0]).isCloseTo(-10.0, within(0.01));
        assertThat(vec[1]).isCloseTo(0.0, within(0.01));

        // 36 km/h = 10 m/s; wind from North (0 deg) -> blowing South (u = 0, v = -10)
        double[] vecNorth = featureService.deriveWindVectors(36.0, 0.0);
        assertThat(vecNorth[0]).isCloseTo(0.0, within(0.01));
        assertThat(vecNorth[1]).isCloseTo(-10.0, within(0.01));

        // Calm wind threshold: speed < 0.2 m/s (0.5 km/h = 0.138 m/s) -> u = 0, v = 0
        double[] vecCalm = featureService.deriveWindVectors(0.5, 180.0);
        assertThat(vecCalm[0]).isEqualTo(0.0);
        assertThat(vecCalm[1]).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Contract 8: Spatial lag leave-one-out calculation eliminates self-information leakage")
    void testLeaveOneOutSpatialLag() {
        // 3 stations with PM2.5: 50.0, 60.0, 70.0
        List<Double> pm25List = List.of(50.0, 60.0, 70.0);

        // Leave-one-out for station 0 (50.0): mean of [60.0, 70.0] = 65.0
        double lag0 = featureService.calculateLeaveOneOutSpatialLag(pm25List, 0);
        assertThat(lag0).isEqualTo(65.0);

        // Leave-one-out for station 1 (60.0): mean of [50.0, 70.0] = 60.0
        double lag1 = featureService.calculateLeaveOneOutSpatialLag(pm25List, 1);
        assertThat(lag1).isEqualTo(60.0);

        // Single station fallback: own value
        double singleLag = featureService.calculateLeaveOneOutSpatialLag(List.of(45.0), 0);
        assertThat(singleLag).isEqualTo(45.0);
    }

    @Test
    @DisplayName("Contract 9 & 10: Monitoring network distance and 7.0km coverage gap rule")
    void testMonitoringCoverageGap() {
        double stationLat = 18.5314;
        double stationLon = 73.8446;

        // Neighbor station at 10.0 km (> 7.0 km threshold)
        // Lat delta ~ 0.09 deg ~= 10 km
        List<double[]> distantStation = List.of(
                new double[]{stationLat, stationLon}, // self
                new double[]{18.6214, 73.8446}        // ~10 km away
        );

        Map<String, Object> distantRes = featureService.computeMonitoringNetworkFeatures(stationLat, stationLon, distantStation);
        assertThat((Double) distantRes.get("nearest_station_distance_km")).isGreaterThan(7.0);
        assertThat((Integer) distantRes.get("monitoring_coverage_gap_flag")).isEqualTo(1);

        // Neighbor station at 3.0 km (<= 7.0 km threshold)
        List<double[]> closeStation = List.of(
                new double[]{stationLat, stationLon}, // self
                new double[]{18.5584, 73.8446}        // ~3 km away
        );

        Map<String, Object> closeRes = featureService.computeMonitoringNetworkFeatures(stationLat, stationLon, closeStation);
        assertThat((Double) closeRes.get("nearest_station_distance_km")).isLessThanOrEqualTo(7.0);
        assertThat((Integer) closeRes.get("monitoring_coverage_gap_flag")).isEqualTo(0);
        assertThat((Integer) closeRes.get("stations_within_5km_count")).isEqualTo(2); // self + neighbor
    }

    @Test
    @DisplayName("Contract 11: Cyclical temporal features follow Asia/Kolkata solar time")
    void testTemporalFeaturesGeneration() {
        // 2026-09-26 06:30:00 UTC -> 12:00:00 IST (noon)
        Instant utcNoonIst = Instant.parse("2026-09-26T06:30:00Z");
        Map<String, Object> temporal = featureService.computeTemporalFeatures(utcNoonIst);

        assertThat((Integer) temporal.get("hour")).isEqualTo(12);
        // At 12:00, hour_sin = sin(2*pi*12/24) = sin(pi) = 0.0, hour_cos = cos(pi) = -1.0
        assertThat((Double) temporal.get("hour_sin")).isCloseTo(0.0, within(0.01));
        assertThat((Double) temporal.get("hour_cos")).isCloseTo(-1.0, within(0.01));
        // 2026-09-26 is a Saturday (dow = 5) -> is_weekend = 1
        assertThat((Integer) temporal.get("is_weekend")).isEqualTo(1);
    }

    @Test
    @DisplayName("Contract 12 & 13: NASA FIRMS 24h lookback and zero-fill on fire absence")
    void testFireFeaturesZeroFillOnAbsence() {
        Map<String, Object> fireRes = featureService.computeFireFeatures(18.53, 73.84, 2.5, 90.0, Collections.emptyList());

        assertThat((Integer) fireRes.get("fire_count_24h_25km")).isEqualTo(0);
        assertThat((Double) fireRes.get("fire_frp_sum_24h_25km")).isEqualTo(0.0);
        assertThat((Double) fireRes.get("fire_frp_mean_24h_25km")).isEqualTo(0.0);
        assertThat((Double) fireRes.get("nearest_fire_distance_km")).isEqualTo(50.0);
        assertThat((Double) fireRes.get("fire_frp_distance_decay")).isEqualTo(0.0);
        assertThat((Double) fireRes.get("fire_upwind_alignment_score")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Contract 17 & 18: GIS proximity features use Member 3 regional baselines")
    void testGisFeaturesBaselines() {
        Map<String, Object> gis = featureService.computeGisFeatures(18.53, 73.84);

        assertThat((Double) gis.get("dist_to_nearest_industrial_km")).isEqualTo(3.50);
        assertThat((Double) gis.get("dist_to_nearest_major_road_km")).isEqualTo(0.40);
        assertThat((Integer) gis.get("sensitive_receptors_count_2km")).isEqualTo(4);
        assertThat((Integer) gis.get("industrial_zone_within_2km_flag")).isEqualTo(0);
    }

    @Test
    @DisplayName("Contract 20: Deterministic feature vector generation")
    void testDeterministicOutput() {
        Instant t = Instant.parse("2026-09-26T06:00:00Z");
        Map<String, Object> run1 = featureService.computeTemporalFeatures(t);
        Map<String, Object> run2 = featureService.computeTemporalFeatures(t);

        assertThat(run1).isEqualTo(run2);
    }
}
