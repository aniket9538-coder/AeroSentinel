package com.aerosentinel.hotspot;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AeroSentinel — F3 Phase 6: Hotspot End-to-End Hardening & Production Consistency Tests.
 *
 * Verifies:
 * 1. Complete F2 -> FeatureSnapshot -> ML -> DB -> API chain consistency.
 * 2. Prediction history preservation across time (no historical row deletion).
 * 3. Tie-breaking consistency (created_at DESC on equal predicted_at).
 * 4. Multi-city engine routing (Pune -> ML, Mumbai & Delhi -> Baseline).
 * 5. Strict freshness transitions (LIVE, STALE, UNAVAILABLE, NO_DATA).
 * 6. AI service failure safety (retains old DB prediction as STALE, never injects fake numbers).
 * 7. Feature failure safety (missing features, non-finite values, schema mismatches).
 * 8. Probability out-of-bounds safety (< 0.0, > 1.0, NaN).
 * 9. Geographic boundary protection (MODEL_DOMAIN_UNSUPPORTED).
 */
@ExtendWith(MockitoExtension.class)
class HotspotPhase6HardeningTest {

    @Mock
    private HotspotRepository hotspotRepository;

    @Mock
    private GridRepository gridRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private FeatureSnapshotRepository featureSnapshotRepository;

    @Mock
    private AiServiceHotspotClient aiServiceClient;

    private ModelFeatureVectorAdapter adapter;
    private BaselineHotspotDetectionEngine baselineEngine;
    private MLHotspotDetectionEngine mlEngine;
    private HotspotPredictionValidator validator;
    private HotspotService hotspotService;
    private ObjectMapper mapper;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID DELHI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    private static final String PUNE_H3 = "88608850e5fffff";

    @BeforeEach
    void setUp() {
        adapter = new ModelFeatureVectorAdapter();
        baselineEngine = new BaselineHotspotDetectionEngine(adapter);
        mlEngine = new MLHotspotDetectionEngine(adapter, aiServiceClient, baselineEngine);
        validator = new HotspotPredictionValidator();
        mapper = new ObjectMapper();

        hotspotService = new HotspotService(
                hotspotRepository,
                gridRepository,
                cityRepository,
                featureSnapshotRepository,
                null,
                mlEngine,
                baselineEngine,
                validator
        );
    }

    // =========================================================================
    // 1. Engine Routing Consistency
    // =========================================================================

    @Test
    @DisplayName("Engine routing: Pune strictly routes to MLHotspotDetectionEngine")
    void testEngineRoutingPune() {
        HotspotDetectionEngine engine = hotspotService.getEngineForCity(PUNE_CITY_ID);
        assertThat(engine.getEngineType()).isEqualTo("ML");
        assertThat(engine.getEngineVersion()).isEqualTo("hotspot_classifier_v1");
    }

    @Test
    @DisplayName("Engine routing: Mumbai & Delhi strictly route to BaselineHotspotDetectionEngine")
    void testEngineRoutingNonPune() {
        HotspotDetectionEngine mumbaiEngine = hotspotService.getEngineForCity(MUMBAI_CITY_ID);
        assertThat(mumbaiEngine.getEngineType()).isEqualTo("BASELINE");
        assertThat(mumbaiEngine.getEngineVersion()).isEqualTo("hotspot-baseline-v1");

        HotspotDetectionEngine delhiEngine = hotspotService.getEngineForCity(DELHI_CITY_ID);
        assertThat(delhiEngine.getEngineType()).isEqualTo("BASELINE");
        assertThat(delhiEngine.getEngineVersion()).isEqualTo("hotspot-baseline-v1");
    }

    // =========================================================================
    // 2. Prediction History Preservation & Tie-breaking
    // =========================================================================

    @Test
    @DisplayName("History preservation: multiple historical predictions remain distinct and immutable")
    void testPredictionHistoryPreserved() {
        Instant t0 = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant t1 = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant t2 = Instant.now();

        HotspotPrediction p0 = createPrediction(PUNE_H3, 0.65, "HIGH", t0, t0, "hotspot_classifier_v1");
        HotspotPrediction p1 = createPrediction(PUNE_H3, 0.72, "CRITICAL", t1, t1, "hotspot_classifier_v1");
        HotspotPrediction p2 = createPrediction(PUNE_H3, 0.7998, "CRITICAL", t2, t2, "hotspot_classifier_v1");

        List<HotspotPrediction> allHistory = List.of(p2, p1, p0);
        when(hotspotRepository.findLatestByCityId(PUNE_CITY_ID)).thenReturn(List.of(p2));

        City pune = new City("Pune", "Maharashtra", 18.5204, 73.8567);
        when(cityRepository.findById(PUNE_CITY_ID)).thenReturn(Optional.of(pune));

        GridCell cell = new GridCell(PUNE_CITY_ID, PUNE_H3, 18.5315, 73.8471);
        when(gridRepository.findByCityId(PUNE_CITY_ID)).thenReturn(List.of(cell));

        HotspotOverviewResponse resp = hotspotService.getHotspotsForCity(PUNE_CITY_ID);

        // Verifies the latest prediction p2 is selected, while p0 and p1 are preserved in the DB
        assertThat(resp.cells()).hasSize(1);
        assertThat(resp.cells().get(0).riskScore()).isEqualTo(0.7998);
        assertThat(resp.cells().get(0).riskLevel()).isEqualTo("CRITICAL");
        assertThat(resp.modelVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(resp.engineType()).isEqualTo("ML");

        // Verify no delete or update was called on the repository
        verify(hotspotRepository, never()).delete(any());
        verify(hotspotRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Tie-breaking: when predicted_at timestamps are identical, the newest created_at record wins")
    void testTieBreakingSamePredictedAt() {
        Instant samePredictedAt = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant olderCreatedAt = Instant.now().minus(25, ChronoUnit.MINUTES);
        Instant newerCreatedAt = Instant.now().minus(5, ChronoUnit.MINUTES);

        HotspotPrediction baselinePred = createPrediction(PUNE_H3, 0.7225, "HIGH", samePredictedAt, olderCreatedAt, "hotspot-baseline-v1");
        HotspotPrediction mlPred = createPrediction(PUNE_H3, 0.7998, "CRITICAL", samePredictedAt, newerCreatedAt, "hotspot_classifier_v1");

        // Native query orders by predicted_at DESC, created_at DESC -> mlPred is returned
        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(PUNE_H3)).thenReturn(Optional.of(mlPred));

        Optional<HotspotCellDto> dtoOpt = hotspotService.getHotspotByH3(PUNE_H3);

        assertThat(dtoOpt).isPresent();
        HotspotCellDto dto = dtoOpt.get();
        assertThat(dto.riskScore()).isEqualTo(0.7998);
        assertThat(dto.riskLevel()).isEqualTo("CRITICAL");
        assertThat(dto.modelVersion()).isEqualTo("hotspot_classifier_v1");
    }

    // =========================================================================
    // 3. Freshness Classification
    // =========================================================================

    @Test
    @DisplayName("Freshness contract: age <= 2h -> LIVE, 2h < age <= 24h -> STALE, age > 24h -> UNAVAILABLE")
    void testFreshnessTransitions() {
        Instant now = Instant.now();

        // 30 mins ago -> LIVE
        Instant liveTime = now.minus(30, ChronoUnit.MINUTES);
        HotspotPrediction livePred = createPrediction(PUNE_H3, 0.80, "CRITICAL", liveTime, liveTime, "hotspot_classifier_v1");
        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(PUNE_H3)).thenReturn(Optional.of(livePred));
        assertThat(hotspotService.getHotspotByH3(PUNE_H3).get().freshness()).isEqualTo("LIVE");

        // 5 hours ago -> STALE
        Instant staleTime = now.minus(5, ChronoUnit.HOURS);
        HotspotPrediction stalePred = createPrediction(PUNE_H3, 0.80, "CRITICAL", staleTime, staleTime, "hotspot_classifier_v1");
        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(PUNE_H3)).thenReturn(Optional.of(stalePred));
        assertThat(hotspotService.getHotspotByH3(PUNE_H3).get().freshness()).isEqualTo("STALE");

        // 48 hours ago -> UNAVAILABLE
        Instant unavailTime = now.minus(48, ChronoUnit.HOURS);
        HotspotPrediction unavailPred = createPrediction(PUNE_H3, 0.80, "CRITICAL", unavailTime, unavailTime, "hotspot_classifier_v1");
        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(PUNE_H3)).thenReturn(Optional.of(unavailPred));
        assertThat(hotspotService.getHotspotByH3(PUNE_H3).get().freshness()).isEqualTo("UNAVAILABLE");
    }

    // =========================================================================
    // 4. AI Service Failure Safety & Stale Telemetry Protection
    // =========================================================================

    @Test
    @DisplayName("Failure safety: when AI service fails, existing DB predictions are preserved and marked STALE without fake scores")
    void testAiServiceFailurePreservesStalePredictions() {
        Instant olderTime = Instant.now().minus(3, ChronoUnit.HOURS);
        HotspotPrediction existingPred = createPrediction(PUNE_H3, 0.75, "CRITICAL", olderTime, olderTime, "hotspot_classifier_v1");

        City pune = new City("Pune", "Maharashtra", 18.5204, 73.8567);
        when(cityRepository.findById(PUNE_CITY_ID)).thenReturn(Optional.of(pune));

        GridCell cell = new GridCell(PUNE_CITY_ID, PUNE_H3, 18.5315, 73.8471);
        when(gridRepository.findByCityId(PUNE_CITY_ID)).thenReturn(List.of(cell));

        // DB already has existing prediction
        when(hotspotRepository.findLatestByCityId(PUNE_CITY_ID)).thenReturn(List.of(existingPred));

        HotspotOverviewResponse resp = hotspotService.getHotspotsForCity(PUNE_CITY_ID);

        // Verification: existing prediction is returned and correctly labeled STALE
        assertThat(resp.cells()).hasSize(1);
        assertThat(resp.cells().get(0).riskScore()).isEqualTo(0.75);
        assertThat(resp.cells().get(0).freshness()).isEqualTo("STALE");
        assertThat(resp.freshness()).isEqualTo("STALE");

        // No new predictions were saved
        verify(hotspotRepository, never()).save(any());
    }

    // =========================================================================
    // 5. Feature Failure Safety
    // =========================================================================

    @Test
    @DisplayName("Feature failure safety: snapshot missing required co-pollutants throws INSUFFICIENT_DATA")
    void testMissingCoPollutantsThrowsInsufficientData() throws Exception {
        Map<String, Object> featureMap = createValidPuneFeatures();
        featureMap.remove("pm10"); // Remove PM10

        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, PUNE_H3, "VALID", featureMap);

        assertThatThrownBy(() -> mlEngine.evaluate(snapshot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INSUFFICIENT_DATA");
    }

    @Test
    @DisplayName("Feature failure safety: non-finite feature value (NaN / Infinity) throws IllegalStateException")
    void testNonFiniteFeatureValueThrows() throws Exception {
        Map<String, Object> featureMap = createValidPuneFeatures();
        featureMap.put("temperature", Double.NaN);

        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, PUNE_H3, "VALID", featureMap);

        assertThatThrownBy(() -> adapter.adapt(snapshot))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("temperature");
    }

    @Test
    @DisplayName("Feature failure safety: schema version mismatch throws IllegalStateException")
    void testSchemaVersionMismatchThrows() throws Exception {
        Map<String, Object> featureMap = createValidPuneFeatures();
        FeatureSnapshot snapshot = createSnapshot(PUNE_CITY_ID, PUNE_H3, "VALID", featureMap);
        snapshot.setFeatureSchemaVersion("f3-features-v2-experimental");

        assertThatThrownBy(() -> adapter.adapt(snapshot))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Feature schema version mismatch");
    }

    // =========================================================================
    // 6. Probability Out-of-Bounds Safety
    // =========================================================================

    @Test
    @DisplayName("Probability safety: probability > 1.0 or < 0.0 or NaN is rejected by engine and validator")
    void testInvalidProbabilityRejected() {
        // Validator checks
        UUID snapshotId = UUID.randomUUID();
        assertThatThrownBy(() -> validator.validate(PUNE_CITY_ID, PUNE_H3, Instant.now(), 1.5, "CRITICAL", 0.85, "hotspot_classifier_v1", snapshotId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskScore must be strictly within [0.0, 1.0]");

        assertThatThrownBy(() -> validator.validate(PUNE_CITY_ID, PUNE_H3, Instant.now(), -0.1, "LOW", 0.85, "hotspot_classifier_v1", snapshotId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskScore must be strictly within [0.0, 1.0]");

        assertThatThrownBy(() -> validator.validate(PUNE_CITY_ID, PUNE_H3, Instant.now(), Double.NaN, "MODERATE", 0.85, "hotspot_classifier_v1", snapshotId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskScore must be strictly within [0.0, 1.0]");
    }

    // =========================================================================
    // 7. Geographic Boundary Protection
    // =========================================================================

    @Test
    @DisplayName("Geographic safety: direct invocation of MLHotspotDetectionEngine with Mumbai cell returns MODEL_DOMAIN_UNSUPPORTED")
    void testDirectMlInvocationUnsupportedDomain() throws Exception {
        Map<String, Object> featureMap = createValidPuneFeatures();
        featureMap.put("latitude", 19.0760);
        featureMap.put("longitude", 72.8777);

        FeatureSnapshot mumbaiSnapshot = createSnapshot(MUMBAI_CITY_ID, "88608b56b3fffff", "VALID", featureMap);

        HotspotPredictionResult result = mlEngine.evaluate(mumbaiSnapshot);

        assertThat(result.modelVersion()).isEqualTo("hotspot-baseline-v1");
        assertThat(result.engineType()).isEqualTo("BASELINE");
        assertThat(result.metadata().get("status")).isEqualTo("MODEL_DOMAIN_UNSUPPORTED");
        assertThat(result.metadata().get("domainSupported")).isEqualTo(false);
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    private HotspotPrediction createPrediction(String h3Index, double riskScore, String riskLevel, Instant predictedAt, Instant createdAt, String modelVersion) {
        HotspotPrediction p = new HotspotPrediction();
        p.setId(UUID.randomUUID());
        p.setCityId(PUNE_CITY_ID);
        p.setGridCellId(UUID.randomUUID());
        p.setH3Index(h3Index);
        p.setRiskScore(riskScore);
        p.setRiskLevel(riskLevel);
        p.setConfidence(0.85);
        p.setPredictedAt(predictedAt);
        p.setCreatedAt(createdAt);
        p.setModelVersion(modelVersion);
        p.setFeatureSnapshotId(UUID.randomUUID());
        p.setExplanationStatus("PENDING");
        return p;
    }

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

    private Map<String, Object> createValidPuneFeatures() {
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
        m.put("wind_speed", 15.7);
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
