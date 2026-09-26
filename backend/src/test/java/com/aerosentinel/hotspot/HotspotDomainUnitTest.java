package com.aerosentinel.hotspot;

import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HotspotDomainUnitTest {

    private HotspotPredictionValidator validator;
    private ModelFeatureVectorAdapter adapter;
    private BaselineHotspotDetectionEngine engine;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        validator = new HotspotPredictionValidator();
        adapter = new ModelFeatureVectorAdapter();
        engine = new BaselineHotspotDetectionEngine(adapter);
        mapper = new ObjectMapper();
    }

    // =========================================================================
    // 1. Domain Risk Level & Prediction Validation
    // =========================================================================

    @Test
    @DisplayName("HotspotRiskLevel correctly maps score thresholds")
    void testRiskLevelThresholds() {
        assertThat(HotspotRiskLevel.fromScore(0.95)).isEqualTo(HotspotRiskLevel.CRITICAL);
        assertThat(HotspotRiskLevel.fromScore(0.85)).isEqualTo(HotspotRiskLevel.CRITICAL);
        assertThat(HotspotRiskLevel.fromScore(0.84)).isEqualTo(HotspotRiskLevel.HIGH);
        assertThat(HotspotRiskLevel.fromScore(0.65)).isEqualTo(HotspotRiskLevel.HIGH);
        assertThat(HotspotRiskLevel.fromScore(0.64)).isEqualTo(HotspotRiskLevel.MODERATE);
        assertThat(HotspotRiskLevel.fromScore(0.40)).isEqualTo(HotspotRiskLevel.MODERATE);
        assertThat(HotspotRiskLevel.fromScore(0.39)).isEqualTo(HotspotRiskLevel.LOW);
        assertThat(HotspotRiskLevel.fromScore(0.00)).isEqualTo(HotspotRiskLevel.LOW);
    }

    @Test
    @DisplayName("HotspotPredictionValidator passes on valid parameters")
    void testValidatorPassesOnValidInput() {
        validator.validate(
                UUID.randomUUID(),
                "88608850e5fffff",
                Instant.now(),
                0.72,
                "HIGH",
                0.85,
                "hotspot-baseline-v1",
                UUID.randomUUID()
        );
    }

    @Test
    @DisplayName("HotspotPredictionValidator rejects out-of-range riskScore")
    void testValidatorRejectsInvalidRiskScore() {
        assertThatThrownBy(() -> validator.validate(
                UUID.randomUUID(), "88608850e5fffff", Instant.now(), 1.05, "CRITICAL", 0.85, "v1", UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("riskScore");

        assertThatThrownBy(() -> validator.validate(
                UUID.randomUUID(), "88608850e5fffff", Instant.now(), -0.1, "LOW", 0.85, "v1", UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("riskScore");
    }

    @Test
    @DisplayName("HotspotPredictionValidator rejects out-of-range confidence")
    void testValidatorRejectsInvalidConfidence() {
        assertThatThrownBy(() -> validator.validate(
                UUID.randomUUID(), "88608850e5fffff", Instant.now(), 0.5, "MODERATE", 1.2, "v1", UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("confidence");
    }

    @Test
    @DisplayName("HotspotPredictionValidator rejects future timestamps")
    void testValidatorRejectsFutureTimestamp() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);
        assertThatThrownBy(() -> validator.validate(
                UUID.randomUUID(), "88608850e5fffff", futureTime, 0.5, "MODERATE", 0.85, "v1", UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("future");
    }

    @Test
    @DisplayName("HotspotPredictionValidator rejects invalid H3 index")
    void testValidatorRejectsInvalidH3() {
        assertThatThrownBy(() -> validator.validate(
                UUID.randomUUID(), "invalid", Instant.now(), 0.5, "MODERATE", 0.85, "v1", UUID.randomUUID()
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("h3Index");
    }

    @Test
    @DisplayName("HotspotPredictionValidator rejects null feature snapshot reference")
    void testValidatorRejectsNullSnapshotReference() {
        assertThatThrownBy(() -> validator.validate(
                UUID.randomUUID(), "88608850e5fffff", Instant.now(), 0.5, "MODERATE", 0.85, "v1", null
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("featureSnapshotId");
    }

    // =========================================================================
    // 2. Feature Integration & Adapter Contract
    // =========================================================================

    @Test
    @DisplayName("ModelFeatureVectorAdapter extracts exact 36 features in authoritative order")
    void testAdapterFeatureExtraction() throws Exception {
        Map<String, Object> featureMap = new HashMap<>();
        for (int i = 0; i < FeatureRecord.ORDERED_FEATURE_NAMES.size(); i++) {
            featureMap.put(FeatureRecord.ORDERED_FEATURE_NAMES.get(i), (double) (i + 1));
        }

        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setH3Index("88608850e5fffff");
        snapshot.setObservedAt(Instant.now());
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setFeatures(mapper.writeValueAsString(featureMap));
        snapshot.setQualityStatus("VALID");
        snapshot.setMissingFeatures(new String[0]);

        ModelFeatureVectorAdapter.AdaptedFeatureVector adapted = adapter.adapt(snapshot);
        assertThat(adapted.orderedValues()).hasSize(36);
        assertThat(adapted.isFullyValid()).isTrue();

        for (int i = 0; i < 36; i++) {
            assertThat(adapted.orderedValues()[i]).isEqualTo((double) (i + 1));
        }
    }

    @Test
    @DisplayName("ModelFeatureVectorAdapter rejects schema version mismatch")
    void testAdapterRejectsWrongSchema() {
        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setFeatureSchemaVersion("old-schema-v0");
        assertThatThrownBy(() -> adapter.adapt(snapshot))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schema version mismatch");
    }

    // =========================================================================
    // 3. Unit Contract & Wind Speed Normalization
    // =========================================================================

    @Test
    @DisplayName("Wind speed normalization converts km/h to m/s exactly once without double conversion")
    void testWindSpeedNormalization() throws Exception {
        Map<String, Object> featureMap = new HashMap<>();
        for (String feat : FeatureRecord.ORDERED_FEATURE_NAMES) {
            featureMap.put(feat, 1.0);
        }
        // Set wind_speed = 36.0 km/h (expected: 10.0 m/s)
        featureMap.put("wind_speed", 36.0);

        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setH3Index("88608850e5fffff");
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setFeatures(mapper.writeValueAsString(featureMap));
        snapshot.setQualityStatus("VALID");
        snapshot.setMissingFeatures(new String[0]);

        // Base adaptation preserves source unit
        ModelFeatureVectorAdapter.AdaptedFeatureVector rawVec = adapter.adapt(snapshot);
        int wsIndex = FeatureRecord.ORDERED_FEATURE_NAMES.indexOf("wind_speed");
        assertThat(rawVec.orderedValues()[wsIndex]).isEqualTo(36.0);

        // Normalized adaptation converts to m/s
        ModelFeatureVectorAdapter.AdaptedFeatureVector normVec = adapter.adaptWithWindNormalization(snapshot, true);
        assertThat(normVec.orderedValues()[wsIndex]).isEqualTo(10.0);

        // Calling without normalization flag does not convert twice
        ModelFeatureVectorAdapter.AdaptedFeatureVector unnormVec = adapter.adaptWithWindNormalization(snapshot, false);
        assertThat(unnormVec.orderedValues()[wsIndex]).isEqualTo(36.0);
    }

    // =========================================================================
    // 4. Baseline Detection Engine Behavior
    // =========================================================================

    @Test
    @DisplayName("BaselineHotspotDetectionEngine produces deterministic risk scores")
    void testBaselineEngineDeterministic() throws Exception {
        Map<String, Object> featureMap = new HashMap<>();
        for (String feat : FeatureRecord.ORDERED_FEATURE_NAMES) {
            featureMap.put(feat, 0.0);
        }
        featureMap.put("pm10", 150.0);
        featureMap.put("no2", 60.0);
        featureMap.put("pm25_spatial_lag_mean", 75.0);
        featureMap.put("wind_speed", 2.0); // Stagnant
        featureMap.put("monitoring_coverage_gap_flag", 0.0);

        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setH3Index("88608850e5fffff");
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setFeatures(mapper.writeValueAsString(featureMap));
        snapshot.setQualityStatus("VALID");
        snapshot.setMissingFeatures(new String[0]);

        HotspotPredictionResult res1 = engine.evaluate(snapshot);
        HotspotPredictionResult res2 = engine.evaluate(snapshot);

        assertThat(res1.riskScore()).isEqualTo(res2.riskScore());
        assertThat(res1.riskScore()).isBetween(0.0, 1.0);
        assertThat(res1.confidence()).isEqualTo(0.85);
        assertThat(res1.engineType()).isEqualTo("BASELINE");
        assertThat(res1.modelVersion()).isEqualTo("hotspot-baseline-v1");
    }

    @Test
    @DisplayName("BaselineHotspotDetectionEngine applies penalty for monitoring coverage gap")
    void testBaselineEngineCoverageGapPenalty() throws Exception {
        Map<String, Object> featureMap = new HashMap<>();
        for (String feat : FeatureRecord.ORDERED_FEATURE_NAMES) {
            featureMap.put(feat, 10.0);
        }
        featureMap.put("monitoring_coverage_gap_flag", 1.0); // Sparse coverage

        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setH3Index("88608850e5fffff");
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setFeatures(mapper.writeValueAsString(featureMap));
        snapshot.setQualityStatus("VALID");
        snapshot.setMissingFeatures(new String[0]);

        HotspotPredictionResult result = engine.evaluate(snapshot);
        // Confidence penalized from 0.85 to 0.70
        assertThat(result.confidence()).isEqualTo(0.70);
    }

    @Test
    @DisplayName("BaselineHotspotDetectionEngine handles UNAVAILABLE telemetry with degraded confidence")
    void testBaselineEngineUnavailableTelemetry() throws Exception {
        Map<String, Object> featureMap = new HashMap<>();
        for (String feat : FeatureRecord.ORDERED_FEATURE_NAMES) {
            featureMap.put(feat, 0.0);
        }

        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setH3Index("88608b56b3fffff");
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setFeatures(mapper.writeValueAsString(featureMap));
        snapshot.setQualityStatus("UNAVAILABLE");
        snapshot.setMissingFeatures(new String[]{"pm10", "no2", "so2", "co", "o3"});

        HotspotPredictionResult result = engine.evaluate(snapshot);
        assertThat(result.confidence()).isEqualTo(0.35); // Explicit low confidence
        assertThat(result.metadata().get("evaluationMode")).isEqualTo("DEGRADED_SPATIAL_FALLBACK");
    }
}
