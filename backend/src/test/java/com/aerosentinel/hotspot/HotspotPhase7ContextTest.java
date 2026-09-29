package com.aerosentinel.hotspot;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import com.aerosentinel.weather.WeatherObservation;
import com.aerosentinel.weather.WeatherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * AeroSentinel — F3 Phase 7: Hotspot Spatial Context & Cross-Feature Integration Contract Tests.
 *
 * Verifies:
 * 1. Single spatial anchor: Uber H3 index (Res 8) connects all multi-sensor telemetry without secondary location IDs.
 * 2. Prediction -> Spatial Context mapping: complete context object with stable IDs for F4 & F5 attachment.
 * 3. Feature snapshot traceability: persistent feature_snapshot_id link is preserved.
 * 4. Downstream attachment points: predictionId, h3Index, cityId, predictedAt are stable and non-null.
 * 5. Explicit data quality contract: VALID, MISSING, UNAVAILABLE (no fake values).
 * 6. Multi-city engine transparency: ML metadata for Pune, Baseline metadata for Mumbai/Delhi.
 * 7. Selected-cell DTO consistency: GET /api/v1/hotspots/{h3Index} exposes safe product-level context.
 */
@ExtendWith(MockitoExtension.class)
class HotspotPhase7ContextTest {

    @Mock
    private HotspotRepository hotspotRepository;

    @Mock
    private FeatureSnapshotRepository featureSnapshotRepository;

    @Mock
    private AirObservationRepository airObservationRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private GridRepository gridRepository;

    private HotspotContextService contextService;
    private HotspotService hotspotService;
    private ObjectMapper mapper;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final String PUNE_H3 = "88608850e5fffff";
    private static final String MUMBAI_H3 = "88608b56b3fffff";

    @BeforeEach
    void setUp() {
        contextService = new HotspotContextService(
                hotspotRepository,
                featureSnapshotRepository,
                airObservationRepository,
                weatherRepository,
                cityRepository
        );

        hotspotService = new HotspotService(
                hotspotRepository,
                gridRepository,
                cityRepository,
                featureSnapshotRepository,
                null,
                null,
                null,
                null,
                contextService
        );

        mapper = new ObjectMapper();
    }

    // =========================================================================
    // 1. Single Spatial Anchor & Context Derivation
    // =========================================================================

    @Test
    @DisplayName("Single spatial anchor: H3 cell index connects all multi-sensor telemetry without secondary IDs")
    void testSingleSpatialAnchorH3Continuity() throws Exception {
        UUID predictionId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant now = Instant.now();

        HotspotPrediction pred = createPrediction(predictionId, PUNE_CITY_ID, PUNE_H3, 0.7998, "CRITICAL", 0.86, "hotspot_classifier_v1", now, snapshotId);
        FeatureSnapshot snapshot = createSnapshot(snapshotId, PUNE_CITY_ID, PUNE_H3, createPuneFeatureMap());
        City pune = new City("Pune", "Maharashtra", 18.5204, 73.8567);

        when(cityRepository.findById(PUNE_CITY_ID)).thenReturn(Optional.of(pune));
        when(featureSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(snapshot));

        HotspotSpatialContext context = contextService.buildSpatialContext(pred);

        // Verification: H3 index is the primary spatial key
        assertThat(context.h3Index()).isEqualTo(PUNE_H3);
        assertThat(context.predictionId()).isEqualTo(predictionId);
        assertThat(context.cityId()).isEqualTo(PUNE_CITY_ID);
        assertThat(context.cityName()).isEqualTo("Pune");
        assertThat(context.featureSnapshotId()).isEqualTo(snapshotId);
        assertThat(context.engineType()).isEqualTo("ML");
        assertThat(context.modelVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(context.riskScore()).isEqualTo(0.7998);
        assertThat(context.riskLevel()).isEqualTo("CRITICAL");
        assertThat(context.confidence()).isEqualTo(0.86);
        assertThat(context.freshness()).isEqualTo("LIVE");
    }

    // =========================================================================
    // 2. Downstream Attachment Points for F4 & F5
    // =========================================================================

    @Test
    @DisplayName("F4 Forecast & F5 Evidence attachment points: stable IDs and real contextual sub-records")
    void testDownstreamAttachmentPoints() throws Exception {
        UUID predictionId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant obsTime = Instant.now().minus(20, ChronoUnit.MINUTES);

        HotspotPrediction pred = createPrediction(predictionId, PUNE_CITY_ID, PUNE_H3, 0.7998, "CRITICAL", 0.86, "hotspot_classifier_v1", obsTime, snapshotId);
        FeatureSnapshot snapshot = createSnapshot(snapshotId, PUNE_CITY_ID, PUNE_H3, createPuneFeatureMap());

        when(featureSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(snapshot));

        HotspotSpatialContext context = contextService.buildSpatialContext(pred);

        // 1. F4 Attachment Contract: predictionId + h3Index + cityId + predictedAt
        assertThat(context.predictionId()).isNotNull();
        assertThat(context.h3Index()).isNotNull().hasSize(15);
        assertThat(context.cityId()).isNotNull();
        assertThat(context.predictedAt()).isEqualTo(obsTime);

        // 2. F5 Attachment Contract: sub-contexts populated from real telemetry
        assertThat(context.monitoringCoverage().dataQuality()).isEqualTo("VALID");
        assertThat(context.monitoringCoverage().nearestStationDistanceKm()).isEqualTo(0.27);
        assertThat(context.monitoringCoverage().stationsWithin5kmCount()).isEqualTo(2);

        assertThat(context.spatialDispersion().dataQuality()).isEqualTo("VALID");
        assertThat(context.spatialDispersion().pm25SpatialLagMean()).isEqualTo(78.0);
        assertThat(context.spatialDispersion().windU()).isEqualTo(4.3604);

        assertThat(context.environmentalGis().dataQuality()).isEqualTo("VALID");
        assertThat(context.environmentalGis().distToNearestIndustrialKm()).isEqualTo(3.5);
        assertThat(context.environmentalGis().distToNearestMajorRoadKm()).isEqualTo(0.4);
        assertThat(context.environmentalGis().sensitiveReceptorsCount2km()).isEqualTo(4);
    }

    // =========================================================================
    // 3. Multi-Sensor Telemetry Integration: Physical Air & Weather
    // =========================================================================

    @Test
    @DisplayName("Multi-sensor integration: real AirObservation and WeatherObservation are prioritized when present")
    void testRealAirAndWeatherIntegration() throws Exception {
        UUID predictionId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant now = Instant.now();

        HotspotPrediction pred = createPrediction(predictionId, PUNE_CITY_ID, PUNE_H3, 0.7998, "CRITICAL", 0.86, "hotspot_classifier_v1", now, snapshotId);
        FeatureSnapshot snapshot = createSnapshot(snapshotId, PUNE_CITY_ID, PUNE_H3, createPuneFeatureMap());

        // Real AirObservation
        AirObservation airObs = new AirObservation();
        airObs.setH3Index(PUNE_H3);
        airObs.setStationId("PUN-001");
        airObs.setPm25(84.5);
        airObs.setPm10(142.0);
        airObs.setNo2(38.2);
        airObs.setSo2(14.5);
        airObs.setCo(0.9);
        airObs.setO3(24.0);
        airObs.setObservedAt(now);

        // Real WeatherObservation
        WeatherObservation weatherObs = new WeatherObservation();
        weatherObs.setH3Index(PUNE_H3);
        weatherObs.setTemperature(26.5);
        weatherObs.setHumidity(72.0);
        weatherObs.setWindSpeed(18.0); // km/h -> 5.0 m/s
        weatherObs.setWindDirection(260.0);
        weatherObs.setPressure(948.0);
        weatherObs.setRainfall(0.0);
        weatherObs.setObservedAt(now);

        when(featureSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(snapshot));
        when(airObservationRepository.findFirstByH3IndexOrderByObservedAtDesc(PUNE_H3)).thenReturn(Optional.of(airObs));
        when(weatherRepository.findFirstByH3IndexOrderByObservedAtDesc(PUNE_H3)).thenReturn(Optional.of(weatherObs));

        HotspotSpatialContext context = contextService.buildSpatialContext(pred);

        // Air Verification
        assertThat(context.airContext().dataQuality()).isEqualTo("VALID");
        assertThat(context.airContext().pm25()).isEqualTo(84.5);
        assertThat(context.airContext().stationId()).isEqualTo("PUN-001");

        // Weather Verification (wind speed converted to m/s: 18 km/h -> 5.0 m/s)
        assertThat(context.weatherContext().dataQuality()).isEqualTo("VALID");
        assertThat(context.weatherContext().temperature()).isEqualTo(26.5);
        assertThat(context.weatherContext().windSpeedKmh()).isEqualTo(18.0);
        assertThat(context.weatherContext().windSpeedMps()).isEqualTo(5.0);
    }

    // =========================================================================
    // 4. Explicit Data Quality Contract (No Fake Values)
    // =========================================================================

    @Test
    @DisplayName("Data quality contract: missing sensors produce explicit MISSING / UNAVAILABLE flags without fake numbers")
    void testExplicitDataQualityMissingness() {
        UUID predictionId = UUID.randomUUID();
        HotspotPrediction pred = createPrediction(predictionId, PUNE_CITY_ID, PUNE_H3, 0.50, "HIGH", 0.70, "hotspot_classifier_v1", Instant.now(), null);

        // No feature snapshot, no air observation, no weather observation
        when(featureSnapshotRepository.findTopByH3IndexOrderByObservedAtDesc(PUNE_H3)).thenReturn(Optional.empty());
        when(airObservationRepository.findFirstByH3IndexOrderByObservedAtDesc(PUNE_H3)).thenReturn(Optional.empty());
        when(weatherRepository.findFirstByH3IndexOrderByObservedAtDesc(PUNE_H3)).thenReturn(Optional.empty());

        HotspotSpatialContext context = contextService.buildSpatialContext(pred);

        // Explicit missingness flags
        assertThat(context.airContext().dataQuality()).isEqualTo("MISSING");
        assertThat(context.airContext().pm25()).isNull();
        assertThat(context.weatherContext().dataQuality()).isEqualTo("MISSING");
        assertThat(context.weatherContext().temperature()).isNull();
        assertThat(context.monitoringCoverage().dataQuality()).isEqualTo("UNAVAILABLE");
        assertThat(context.spatialDispersion().dataQuality()).isEqualTo("UNAVAILABLE");
        assertThat(context.environmentalGis().dataQuality()).isEqualTo("UNAVAILABLE");
    }

    // =========================================================================
    // 5. Multi-City Engine Transparency
    // =========================================================================

    @Test
    @DisplayName("Multi-city transparency: Pune displays ML engineType while Mumbai displays BASELINE")
    void testMultiCityEngineTransparency() {
        Instant now = Instant.now();

        // Pune ML prediction
        HotspotPrediction punePred = createPrediction(UUID.randomUUID(), PUNE_CITY_ID, PUNE_H3, 0.7998, "CRITICAL", 0.86, "hotspot_classifier_v1", now, null);
        HotspotSpatialContext puneContext = contextService.buildSpatialContext(punePred);
        assertThat(puneContext.engineType()).isEqualTo("ML");
        assertThat(puneContext.modelVersion()).isEqualTo("hotspot_classifier_v1");

        // Mumbai Baseline prediction
        HotspotPrediction mumbaiPred = createPrediction(UUID.randomUUID(), MUMBAI_CITY_ID, MUMBAI_H3, 0.264, "LOW", 0.35, "hotspot-baseline-v1", now, null);
        HotspotSpatialContext mumbaiContext = contextService.buildSpatialContext(mumbaiPred);
        assertThat(mumbaiContext.engineType()).isEqualTo("BASELINE");
        assertThat(mumbaiContext.modelVersion()).isEqualTo("hotspot-baseline-v1");
    }

    // =========================================================================
    // 6. Selected-Cell DTO Consistency in HotspotService
    // =========================================================================

    @Test
    @DisplayName("Selected-cell consistency: getHotspotByH3 attaches complete HotspotSpatialContext and stable IDs")
    void testSelectedCellDtoConsistency() throws Exception {
        UUID predictionId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant now = Instant.now();

        HotspotPrediction pred = createPrediction(predictionId, PUNE_CITY_ID, PUNE_H3, 0.7998, "CRITICAL", 0.86, "hotspot_classifier_v1", now, snapshotId);
        FeatureSnapshot snapshot = createSnapshot(snapshotId, PUNE_CITY_ID, PUNE_H3, createPuneFeatureMap());
        City pune = new City("Pune", "Maharashtra", 18.5204, 73.8567);

        when(hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(PUNE_H3)).thenReturn(Optional.of(pred));
        when(cityRepository.findById(PUNE_CITY_ID)).thenReturn(Optional.of(pune));
        when(featureSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(snapshot));

        Optional<HotspotCellDto> dtoOpt = hotspotService.getHotspotByH3(PUNE_H3);

        assertThat(dtoOpt).isPresent();
        HotspotCellDto dto = dtoOpt.get();

        // Stable IDs
        assertThat(dto.predictionId()).isEqualTo(predictionId);
        assertThat(dto.h3Index()).isEqualTo(PUNE_H3);
        assertThat(dto.cityId()).isEqualTo(PUNE_CITY_ID);
        assertThat(dto.cityName()).isEqualTo("Pune");
        assertThat(dto.engineType()).isEqualTo("ML");
        assertThat(dto.featureSnapshotId()).isEqualTo(snapshotId);

        // Attached rich spatial context
        assertThat(dto.spatialContext()).isNotNull();
        assertThat(dto.spatialContext().predictionId()).isEqualTo(predictionId);
        assertThat(dto.spatialContext().monitoringCoverage().nearestStationDistanceKm()).isEqualTo(0.27);

        // F3 -> F4 Final Contract Assertions
        assertThat(dto.isHotspot()).isTrue();
        assertThat(dto.operationalThreshold()).isEqualTo(0.20);
        assertThat(dto.spatialContext().isHotspot()).isTrue();
        assertThat(dto.spatialContext().operationalThreshold()).isEqualTo(0.20);
        assertThat(dto.spatialContext().confidenceBreakdown()).isNotNull();
        assertThat(dto.spatialContext().confidenceBreakdown().spatialCoverageConfidence()).isEqualTo(0.95);
        assertThat(dto.spatialContext().monitoringCoverage().spatialCoverageConfidence()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("F3 Contract Lock: Operational threshold strictly derives isHotspot for ML and Baseline engines")
    void testF3ToF4ContractFieldsLocked_ThresholdAndHotspotSemantics() {
        UUID predId1 = UUID.randomUUID();
        UUID predId2 = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        Instant now = Instant.now();

        // 1. ML Engine: p = 0.199 (< 0.20) -> isHotspot = false, threshold = 0.20
        HotspotPrediction predMlBelow = createPrediction(predId1, PUNE_CITY_ID, PUNE_H3, 0.1990, "LOW", 0.85, "hotspot_classifier_v1", now, snapshotId);
        HotspotSpatialContext ctxMlBelow = contextService.buildSpatialContext(predMlBelow);
        assertThat(ctxMlBelow.isHotspot()).isFalse();
        assertThat(ctxMlBelow.operationalThreshold()).isEqualTo(0.20);

        // 2. ML Engine: p = 0.201 (>= 0.20) -> isHotspot = true, threshold = 0.20
        HotspotPrediction predMlAbove = createPrediction(predId1, PUNE_CITY_ID, PUNE_H3, 0.2010, "MODERATE", 0.85, "hotspot_classifier_v1", now, snapshotId);
        HotspotSpatialContext ctxMlAbove = contextService.buildSpatialContext(predMlAbove);
        assertThat(ctxMlAbove.isHotspot()).isTrue();
        assertThat(ctxMlAbove.operationalThreshold()).isEqualTo(0.20);

        // 3. Baseline Engine: p = 0.399 (< 0.40) -> isHotspot = false, threshold = 0.40
        HotspotPrediction predBaseBelow = createPrediction(predId2, PUNE_CITY_ID, PUNE_H3, 0.3990, "LOW", 0.70, "hotspot-baseline-v1", now, snapshotId);
        HotspotSpatialContext ctxBaseBelow = contextService.buildSpatialContext(predBaseBelow);
        assertThat(ctxBaseBelow.isHotspot()).isFalse();
        assertThat(ctxBaseBelow.operationalThreshold()).isEqualTo(0.40);

        // 4. Baseline Engine: p = 0.401 (>= 0.40) -> isHotspot = true, threshold = 0.40
        HotspotPrediction predBaseAbove = createPrediction(predId2, PUNE_CITY_ID, PUNE_H3, 0.4010, "MODERATE", 0.70, "hotspot-baseline-v1", now, snapshotId);
        HotspotSpatialContext ctxBaseAbove = contextService.buildSpatialContext(predBaseAbove);
        assertThat(ctxBaseAbove.isHotspot()).isTrue();
        assertThat(ctxBaseAbove.operationalThreshold()).isEqualTo(0.40);
    }

    // =========================================================================
    // Test Helpers
    // =========================================================================

    private HotspotPrediction createPrediction(UUID id, UUID cityId, String h3Index, double riskScore, String riskLevel, double confidence, String modelVersion, Instant predictedAt, UUID snapshotId) {
        HotspotPrediction p = new HotspotPrediction();
        p.setId(id);
        p.setCityId(cityId);
        p.setGridCellId(UUID.randomUUID());
        p.setH3Index(h3Index);
        p.setRiskScore(riskScore);
        p.setRiskLevel(riskLevel);
        p.setConfidence(confidence);
        p.setPredictedAt(predictedAt);
        p.setCreatedAt(Instant.now());
        p.setModelVersion(modelVersion);
        p.setFeatureSnapshotId(snapshotId);
        p.setExplanationStatus("PENDING");
        return p;
    }

    private FeatureSnapshot createSnapshot(UUID id, UUID cityId, String h3Index, Map<String, Object> featureMap) throws Exception {
        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setId(id);
        snapshot.setCityId(cityId);
        snapshot.setH3Index(h3Index);
        snapshot.setObservedAt(Instant.now());
        snapshot.setFeatureSchemaVersion(FeatureRecord.SCHEMA_VERSION);
        snapshot.setQualityStatus("VALID");
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
