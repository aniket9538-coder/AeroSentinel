package com.aerosentinel.hotspot;

import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MLHotspotDetectionEngineTest {

    private ModelFeatureVectorAdapter adapter;
    private AiServiceHotspotClient aiServiceClient;
    private BaselineHotspotDetectionEngine baselineEngine;
    private MLHotspotDetectionEngine mlEngine;
    private ObjectMapper mapper;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

    @BeforeEach
    void setUp() {
        adapter = new ModelFeatureVectorAdapter();
        // Client configured to use default localhost:8000 with process fallback
        aiServiceClient = new AiServiceHotspotClient("http://localhost:8000", 5000);
        baselineEngine = new BaselineHotspotDetectionEngine(adapter);
        mlEngine = new MLHotspotDetectionEngine(adapter, aiServiceClient, baselineEngine);
        mapper = new ObjectMapper();
    }

    // =========================================================================
    // 1. Engine Contract & Identity
    // =========================================================================

    @Test
    @DisplayName("MLHotspotDetectionEngine correctly implements HotspotDetectionEngine interface")
    void testEngineContract() {
        assertThat(mlEngine).isInstanceOf(HotspotDetectionEngine.class);
        assertThat(mlEngine.getEngineVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(mlEngine.getEngineType()).isEqualTo("ML");
    }

    @Test
    @DisplayName("BaselineHotspotDetectionEngine remains available alongside ML engine")
    void testBaselineEnginePreserved() {
        assertThat(baselineEngine).isInstanceOf(HotspotDetectionEngine.class);
        assertThat(baselineEngine.getEngineVersion()).isEqualTo("hotspot-baseline-v1");
        assertThat(baselineEngine.getEngineType()).isEqualTo("BASELINE");
    }

    // =========================================================================
    // 2. Wind Unit Boundary & Conversion Verification
    // =========================================================================

    @Test
    @DisplayName("Wind unit boundary: 36 km/h converts to 10 m/s exactly once, no double conversion")
    void testWindUnitConversion() throws Exception {
        Map<String, Object> featureMap = createPuneFeatureMap();
        featureMap.put("wind_speed", 36.0); // km/h

        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, "88608850e5fffff", "VALID", featureMap);

        // Adapt with wind normalization = true
        ModelFeatureVectorAdapter.AdaptedFeatureVector adapted =
                adapter.adaptWithWindNormalization(snapshot, true);

        Double convertedWindSpeed = adapted.featureMap().get("wind_speed");
        assertThat(convertedWindSpeed).isEqualTo(10.0);

        // Adapt with wind normalization = false (remains raw km/h)
        ModelFeatureVectorAdapter.AdaptedFeatureVector raw =
                adapter.adaptWithWindNormalization(snapshot, false);
        assertThat(raw.featureMap().get("wind_speed")).isEqualTo(36.0);
    }

    // =========================================================================
    // 3. Exact 36-Feature Vector Contract
    // =========================================================================

    @Test
    @DisplayName("ModelFeatureVectorAdapter extracts exactly 36 features in authoritative order")
    void testExact36FeatureVector() throws Exception {
        Map<String, Object> featureMap = createPuneFeatureMap();
        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, "88608850e5fffff", "VALID", featureMap);

        ModelFeatureVectorAdapter.AdaptedFeatureVector adapted =
                adapter.adaptWithWindNormalization(snapshot, true);

        assertThat(adapted.orderedValues()).hasSize(36);
        assertThat(adapted.featureMap()).hasSize(36);
        assertThat(adapted.isFullyValid()).isTrue();
        assertThat(adapted.missingFeatures()).isEmpty();

        // Verify key feature indices
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES.get(0)).isEqualTo("latitude");
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES.get(1)).isEqualTo("longitude");
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES.get(2)).isEqualTo("pm10");
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES.get(16)).isEqualTo("wind_speed");
        assertThat(FeatureRecord.ORDERED_FEATURE_NAMES.get(35)).isEqualTo("fire_upwind_alignment_score");
    }

    // =========================================================================
    // 4. Real Pune Snapshot Inference & Determinism
    // =========================================================================

    @Test
    @DisplayName("Real Pune snapshot produces calibrated probability and CRITICAL risk level")
    void testRealPuneSnapshotInference() throws Exception {
        Map<String, Object> featureMap = createPuneFeatureMap();
        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, "88608850e5fffff", "VALID", featureMap);

        HotspotPredictionResult result = mlEngine.evaluate(snapshot);

        assertThat(result).isNotNull();
        assertThat(result.modelVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(result.engineType()).isEqualTo("ML");
        assertThat(result.riskScore()).isBetween(0.0, 1.0);
        assertThat(result.riskScore()).isEqualTo(0.7998);
        assertThat(result.riskLevel()).isEqualTo(HotspotRiskLevel.CRITICAL);
        assertThat(result.confidence()).isBetween(0.70, 1.0);
        assertThat(result.metadata()).containsKey("isHotspot");
        assertThat(result.metadata().get("isHotspot")).isEqualTo(true);
    }

    @Test
    @DisplayName("ML inference is deterministic: identical snapshots produce identical probabilities")
    void testDeterministicInference() throws Exception {
        Map<String, Object> featureMap = createPuneFeatureMap();
        FeatureSnapshot s1 = createSnapshot(PUNE_CITY_ID, "88608850e5fffff", "VALID", featureMap);
        FeatureSnapshot s2 = createSnapshot(PUNE_CITY_ID, "88608850e5fffff", "VALID", featureMap);

        HotspotPredictionResult r1 = mlEngine.evaluate(s1);
        HotspotPredictionResult r2 = mlEngine.evaluate(s2);

        assertThat(r1.riskScore()).isEqualTo(r2.riskScore());
        assertThat(r1.riskLevel()).isEqualTo(r2.riskLevel());
        assertThat(r1.confidence()).isEqualTo(r2.confidence());
    }

    // =========================================================================
    // 5. Model Geography & Domain Enforcement
    // =========================================================================

    @Test
    @DisplayName("Geography guard: Mumbai is not validated for Pune ML model; returns controlled MODEL_DOMAIN_UNSUPPORTED state")
    void testGeographyGuardMumbai() throws Exception {
        Map<String, Object> featureMap = createPuneFeatureMap();
        featureMap.put("latitude", 19.0760);
        featureMap.put("longitude", 72.8777);

        FeatureSnapshot mumbaiSnapshot = createSnapshot(MUMBAI_CITY_ID, "88608b56b3fffff", "VALID", featureMap);

        HotspotPredictionResult result = mlEngine.evaluate(mumbaiSnapshot);

        assertThat(result.metadata()).containsKey("domainSupported");
        assertThat(result.metadata().get("domainSupported")).isEqualTo(false);
        assertThat(result.metadata().get("status")).isEqualTo("MODEL_DOMAIN_UNSUPPORTED");
        // Delegated to baseline with baseline version tag
        assertThat(result.modelVersion()).isEqualTo("hotspot-baseline-v1");
    }

    // =========================================================================
    // 6. Risk Level Mapping to Operational Threshold (p >= 0.20)
    // =========================================================================

    @Test
    @DisplayName("HotspotRiskLevel.fromProbability correctly applies operational decision threshold 0.20")
    void testProbabilityRiskLevelMapping() {
        double threshold = 0.20;

        // p < 0.20 -> LOW
        assertThat(HotspotRiskLevel.fromProbability(0.05, threshold)).isEqualTo(HotspotRiskLevel.LOW);
        assertThat(HotspotRiskLevel.fromProbability(0.19, threshold)).isEqualTo(HotspotRiskLevel.LOW);

        // 0.20 <= p < 0.40 -> MODERATE
        assertThat(HotspotRiskLevel.fromProbability(0.20, threshold)).isEqualTo(HotspotRiskLevel.MODERATE);
        assertThat(HotspotRiskLevel.fromProbability(0.35, threshold)).isEqualTo(HotspotRiskLevel.MODERATE);

        // 0.40 <= p < 0.70 -> HIGH
        assertThat(HotspotRiskLevel.fromProbability(0.40, threshold)).isEqualTo(HotspotRiskLevel.HIGH);
        assertThat(HotspotRiskLevel.fromProbability(0.65, threshold)).isEqualTo(HotspotRiskLevel.HIGH);

        // p >= 0.70 -> CRITICAL
        assertThat(HotspotRiskLevel.fromProbability(0.70, threshold)).isEqualTo(HotspotRiskLevel.CRITICAL);
        assertThat(HotspotRiskLevel.fromProbability(0.85, threshold)).isEqualTo(HotspotRiskLevel.CRITICAL);
    }

    // =========================================================================
    // 7. Controlled Comparison: Baseline Engine vs ML Engine
    // =========================================================================

    @Test
    @DisplayName("Controlled comparison: Baseline vs ML on identical real Pune snapshot")
    void testBaselineVsMlComparison() throws Exception {
        Map<String, Object> featureMap = createPuneFeatureMap();
        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, "88608850e5fffff", "VALID", featureMap);

        HotspotPredictionResult baselineResult = baselineEngine.evaluate(snapshot);
        HotspotPredictionResult mlResult = mlEngine.evaluate(snapshot);

        // Baseline: Multi-criteria rule aggregation
        assertThat(baselineResult.modelVersion()).isEqualTo("hotspot-baseline-v1");
        assertThat(baselineResult.engineType()).isEqualTo("BASELINE");
        assertThat(baselineResult.riskScore()).isEqualTo(0.7225);
        assertThat(baselineResult.riskLevel()).isEqualTo(HotspotRiskLevel.HIGH);

        // ML: Platt-calibrated Random Forest
        assertThat(mlResult.modelVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(mlResult.engineType()).isEqualTo("ML");
        assertThat(mlResult.riskScore()).isEqualTo(0.7998);
        assertThat(mlResult.riskLevel()).isEqualTo(HotspotRiskLevel.CRITICAL);

        // Both identify high risk in Shivajinagar cell, with ML providing calibrated probability
        assertThat(baselineResult.riskScore()).isGreaterThan(0.70);
        assertThat(mlResult.riskScore()).isGreaterThan(0.70);
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    private FeatureSnapshot createSnapshot(UUID cityId, String h3Index, String qualityStatus, Map<String, Object> featureMap) throws Exception {
        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setCityId(cityId);
        snapshot.setH3Index(h3Index);
        snapshot.setObservedAt(Instant.now());
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setQualityStatus(qualityStatus);
        snapshot.setFeatures(mapper.writeValueAsString(featureMap));
        snapshot.setMissingFeatures(new String[0]);
        snapshot.setCreatedAt(Instant.now());
        return snapshot;
    }

    private Map<String, Object> createPuneFeatureMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("latitude", 18.5315);
        m.put("longitude", 73.8471);
        m.put("pm10", 120.0);
        m.put("no2", 37.0);
        m.put("so2", 14.0);
        m.put("co", 0.9);
        m.put("o3", 24.0);
        m.put("hour", 18);
        m.put("day_of_week", 5);
        m.put("is_weekend", 1);
        m.put("hour_sin", -1.0);
        m.put("hour_cos", 0.0);
        m.put("dow_sin", -0.9749);
        m.put("dow_cos", -0.2225);
        m.put("temperature", 24.9);
        m.put("humidity", 77.0);
        m.put("wind_speed", 15.7); // km/h
        m.put("wind_direction", 269.0);
        m.put("wind_u", 4.3604);
        m.put("wind_v", 0.0761);
        m.put("rainfall", 0.0);
        m.put("pressure", 947.5);
        m.put("pm25_spatial_lag_mean", 78.0);
        m.put("nearest_station_distance_km", 0.27);
        m.put("stations_within_5km_count", 2);
        m.put("monitoring_coverage_gap_flag", 0);
        m.put("dist_to_nearest_industrial_km", 3.5);
        m.put("dist_to_nearest_major_road_km", 0.4);
        m.put("sensitive_receptors_count_2km", 4);
        m.put("industrial_zone_within_2km_flag", 0);
        m.put("fire_count_24h_25km", 0);
        m.put("fire_frp_sum_24h_25km", 0.0);
        m.put("fire_frp_mean_24h_25km", 0.0);
        m.put("nearest_fire_distance_km", 50.0);
        m.put("fire_frp_distance_decay", 0.0);
        m.put("fire_upwind_alignment_score", 0.0);
        return m;
    }
}
