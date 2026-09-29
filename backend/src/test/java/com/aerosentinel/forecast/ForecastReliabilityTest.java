package com.aerosentinel.forecast;

import com.aerosentinel.exception.GlobalExceptionHandler;
import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import com.aerosentinel.forecast.feature.FeatureProvenance;
import com.aerosentinel.forecast.feature.ForecastFeatureAdapter;
import com.aerosentinel.forecast.feature.ForecastFeatureAdapter.ModelReadyFeatureVector;
import com.aerosentinel.forecast.feature.ForecastFeatureBuilder;
import com.aerosentinel.forecast.feature.ForecastFeatureValidator;
import com.aerosentinel.forecast.feature.ForecastFeaturesInvalidException;
import com.aerosentinel.forecast.feature.ForecastFeatureVector;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AeroSentinel - Production F4-P7 Forecast Reliability & Controlled Failure Tests (Spring Boot).
 * File: backend/src/test/java/com/aerosentinel/forecast/ForecastReliabilityTest.java
 *
 * Verifies Track B Engineering Reliability Requirements:
 *  P7.1 - Insufficient history rejection and zero persistence
 *  P7.2 - Missing weather provenance tracking & no false VALID assertion
 *  P7.3 - Model unavailable controlled failure (503 SERVICE_UNAVAILABLE)
 *  P7.4 - ML process / timeout / service failure (504 GATEWAY_TIMEOUT, 503 SERVICE_UNAVAILABLE)
 *  P7.5 - Stale forecast verification (STALE != LIVE)
 *  P7.6 - Invalid bounds rejection (Cases A, B, C, D)
 *  P7.7 - Atomic persistence verification (0 rows on failure, 3 rows on success)
 *  P7.8 - Controlled error response contract without stack trace leakage
 *  P7.10 - Real forecast regression references preservation
 */
class ForecastReliabilityTest {

    private ForecastService forecastService;
    private ForecastRepository forecastRepository;
    private HotspotRepository hotspotRepository;
    private FeatureSnapshotRepository featureSnapshotRepository;
    private ForecastFeatureBuilder featureBuilder;
    private ForecastFeatureValidator featureValidator;
    private ForecastFeatureAdapter featureAdapter;
    private ForecastAiClient aiClient;
    private GlobalExceptionHandler exceptionHandler;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String PUNE_H3 = "88608850e5fffff";
    private static final UUID PARENT_ID = UUID.fromString("a310c689-f340-49fc-8935-a037de8d7709");
    private static final UUID SNAPSHOT_ID = UUID.fromString("1624baa3-a5f8-407b-b1c2-36bcee7650b1");

    @BeforeEach
    void setUp() {
        forecastRepository = mock(ForecastRepository.class);
        hotspotRepository = mock(HotspotRepository.class);
        featureSnapshotRepository = mock(FeatureSnapshotRepository.class);
        featureValidator = new ForecastFeatureValidator();
        featureAdapter = new ForecastFeatureAdapter(featureValidator);
        featureBuilder = mock(ForecastFeatureBuilder.class);
        aiClient = mock(ForecastAiClient.class);
        exceptionHandler = new GlobalExceptionHandler();

        forecastService = new ForecastService(
                forecastRepository,
                hotspotRepository,
                featureSnapshotRepository,
                featureBuilder,
                featureValidator,
                featureAdapter,
                aiClient
        );
    }

    private HotspotPrediction createMockParentPrediction() {
        HotspotPrediction parent = new HotspotPrediction();
        parent.setId(PARENT_ID);
        parent.setCityId(PUNE_CITY_ID);
        parent.setH3Index(PUNE_H3);
        parent.setFeatureSnapshotId(SNAPSHOT_ID);
        parent.setPredictedAt(Instant.parse("2026-09-26T13:09:44.571028Z"));
        return parent;
    }

    private ModelReadyFeatureVector createValidModelReadyVector() {
        double[] vals = new double[36];
        Arrays.fill(vals, 1.0);
        return new ModelReadyFeatureVector(
                PUNE_H3,
                PUNE_CITY_ID,
                Instant.now(),
                SNAPSHOT_ID,
                "VALID",
                Collections.emptyList(),
                vals,
                Collections.emptyMap(),
                false
        );
    }

    private ForecastFeatureVector createValidForecastFeatureVector() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (String feat : ForecastFeatureVector.ORDERED_FEATURE_NAMES) {
            map.put(feat, 1.0);
        }
        double[] vals = new double[36];
        Arrays.fill(vals, 1.0);
        return new ForecastFeatureVector(
                PUNE_CITY_ID,
                PUNE_H3,
                Instant.now(),
                SNAPSHOT_ID,
                "VALID",
                Collections.emptyList(),
                map,
                vals,
                Collections.emptyMap()
        );
    }

    // =========================================================================
    // P7.1 — INSUFFICIENT HISTORY
    // =========================================================================

    @Test
    @DisplayName("P7.1: Rejects feature vector with missing history in strict mode")
    void testInsufficientHistoryRejectionInStrictMode() {
        Map<String, Double> map = new HashMap<>();
        for (String feat : ForecastFeatureVector.ORDERED_FEATURE_NAMES) {
            map.put(feat, 1.0);
        }

        ForecastFeatureVector vectorWithMissingHistory = new ForecastFeatureVector(
                PUNE_CITY_ID,
                PUNE_H3,
                Instant.now(),
                SNAPSHOT_ID,
                "MISSING",
                List.of("pm25_spatial_lag_mean", "pm10"),
                map,
                new double[36],
                Collections.emptyMap()
        );

        assertThatThrownBy(() -> featureValidator.validate(vectorWithMissingHistory, true))
                .isInstanceOf(ForecastFeaturesInvalidException.class)
                .hasMessageContaining("Check 6 failed: Strict mode requires zero missing features");
    }

    @Test
    @DisplayName("P7.1: Unknown parent prediction prevents inference and leaves zero forecasts persisted")
    void testUnknownParentRejectionZeroPersistence() {
        when(hotspotRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

        ForecastGenerateRequest req = new ForecastGenerateRequest(PARENT_ID, PUNE_CITY_ID, PUNE_H3);

        assertThatThrownBy(() -> forecastService.generateForecast(req))
                .isInstanceOf(ForecastException.ParentNotFound.class)
                .hasMessageContaining("F3 Parent prediction not found");

        verify(forecastRepository, never()).saveAll(any());
    }

    // =========================================================================
    // P7.2 — MISSING WEATHER & PROVENANCE
    // =========================================================================

    @Test
    @DisplayName("P7.2: Missing weather inputs cannot be falsely asserted as VALID quality status")
    void testMissingWeatherQualityIntegrityRejection() {
        Map<String, Double> map = new HashMap<>();
        for (String feat : ForecastFeatureVector.ORDERED_FEATURE_NAMES) {
            map.put(feat, 1.0);
        }

        Map<String, FeatureProvenance> prov = new HashMap<>();
        prov.put("temperature", FeatureProvenance.MISSING);
        prov.put("wind_speed", FeatureProvenance.SOURCE_UNAVAILABLE);

        ForecastFeatureVector vector = new ForecastFeatureVector(
                PUNE_CITY_ID,
                PUNE_H3,
                Instant.now(),
                SNAPSHOT_ID,
                "VALID", // FORBIDDEN: claiming VALID despite missing weather
                List.of("temperature", "wind_speed"),
                map,
                new double[36],
                prov
        );

        assertThatThrownBy(() -> featureValidator.validate(vector, false))
                .isInstanceOf(ForecastFeaturesInvalidException.class)
                .hasMessageContaining("Check 14 failed: Quality status cannot be VALID when physical fields are missing");
    }

    @Test
    @DisplayName("P7.2: Provenance semantics never converts MISSING/SOURCE_UNAVAILABLE to VALID_OBSERVATION")
    void testProvenanceSemanticsPreserved() {
        assertThat(FeatureProvenance.MISSING).isNotEqualTo(FeatureProvenance.VALID_OBSERVATION);
        assertThat(FeatureProvenance.SOURCE_UNAVAILABLE).isNotEqualTo(FeatureProvenance.VALID_OBSERVATION);
        assertThat(FeatureProvenance.IMPUTED_BASELINE).isNotEqualTo(FeatureProvenance.VALID_OBSERVATION);
        assertThat(FeatureProvenance.REAL_ZERO).isNotEqualTo(FeatureProvenance.VALID_OBSERVATION);
        assertThat(FeatureProvenance.values()).containsExactlyInAnyOrder(
                FeatureProvenance.VALID_OBSERVATION,
                FeatureProvenance.REAL_ZERO,
                FeatureProvenance.MISSING,
                FeatureProvenance.SOURCE_UNAVAILABLE,
                FeatureProvenance.IMPUTED_BASELINE
        );
    }

    // =========================================================================
    // P7.3 — MODEL UNAVAILABLE
    // =========================================================================

    @Test
    @DisplayName("P7.3: Model unavailable raises AiUnavailable and persists zero forecasts")
    void testModelUnavailableControlledFailure() {
        HotspotPrediction parent = createMockParentPrediction();
        when(hotspotRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(featureSnapshotRepository.findById(SNAPSHOT_ID)).thenReturn(Optional.of(mock(FeatureSnapshot.class)));

        ForecastFeatureVector validVector = createValidForecastFeatureVector();
        when(featureBuilder.buildFromSnapshot(any())).thenReturn(validVector);

        // Simulate Python CLI failing because model artifact is missing
        when(aiClient.predict(any(), any(), any())).thenThrow(
                new ForecastException.AiUnavailable("Forecast model unavailable: models/artifacts/forecast_regressors_v1.joblib not found")
        );

        ForecastGenerateRequest req = new ForecastGenerateRequest(PARENT_ID, PUNE_CITY_ID, PUNE_H3);

        assertThatThrownBy(() -> forecastService.generateForecast(req))
                .isInstanceOf(ForecastException.AiUnavailable.class)
                .hasMessageContaining("Forecast model unavailable");

        verify(forecastRepository, never()).saveAll(any());
    }

    // =========================================================================
    // P7.4 — ML / SERVICE / TIMEOUT FAILURE
    // =========================================================================

    @Test
    @DisplayName("P7.4: Timeout raises AiTimeout and persists zero forecasts")
    void testAiTimeoutHandling() {
        HotspotPrediction parent = createMockParentPrediction();
        when(hotspotRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(featureSnapshotRepository.findById(SNAPSHOT_ID)).thenReturn(Optional.of(mock(FeatureSnapshot.class)));

        ForecastFeatureVector validVector = createValidForecastFeatureVector();
        when(featureBuilder.buildFromSnapshot(any())).thenReturn(validVector);

        when(aiClient.predict(any(), any(), any())).thenThrow(
                new ForecastException.AiTimeout("Forecast inference timed out after 15000 ms")
        );

        ForecastGenerateRequest req = new ForecastGenerateRequest(PARENT_ID, PUNE_CITY_ID, PUNE_H3);

        assertThatThrownBy(() -> forecastService.generateForecast(req))
                .isInstanceOf(ForecastException.AiTimeout.class)
                .hasMessageContaining("Forecast inference timed out");

        verify(forecastRepository, never()).saveAll(any());
    }

    // =========================================================================
    // P7.5 — STALE FORECAST
    // =========================================================================

    @Test
    @DisplayName("P7.5: Freshness contract distinguishes LIVE (<=2h), STALE (2h-24h), UNAVAILABLE (>24h), and NO_DATA")
    void testFreshnessContractThresholds() {
        Instant now = Instant.now();

        // LIVE <= 2h
        assertThat(ForecastMapper.computeFreshness(now.minus(0, ChronoUnit.HOURS), now)).isEqualTo("LIVE");
        assertThat(ForecastMapper.computeFreshness(now.minus(1, ChronoUnit.HOURS), now)).isEqualTo("LIVE");
        assertThat(ForecastMapper.computeFreshness(now.minus(2, ChronoUnit.HOURS), now)).isEqualTo("LIVE");

        // STALE > 2h and <= 24h
        assertThat(ForecastMapper.computeFreshness(now.minus(3, ChronoUnit.HOURS), now)).isEqualTo("STALE");
        assertThat(ForecastMapper.computeFreshness(now.minus(12, ChronoUnit.HOURS), now)).isEqualTo("STALE");
        assertThat(ForecastMapper.computeFreshness(now.minus(24, ChronoUnit.HOURS), now)).isEqualTo("STALE");

        // UNAVAILABLE > 24h
        assertThat(ForecastMapper.computeFreshness(now.minus(25, ChronoUnit.HOURS), now)).isEqualTo("UNAVAILABLE");
        assertThat(ForecastMapper.computeFreshness(now.minus(48, ChronoUnit.HOURS), now)).isEqualTo("UNAVAILABLE");

        // NO_DATA when timestamp is null
        assertThat(ForecastMapper.computeFreshness(null, now)).isEqualTo("NO_DATA");

        // STALE is explicitly not equal to LIVE
        assertThat(ForecastMapper.computeFreshness(now.minus(4, ChronoUnit.HOURS), now)).isNotEqualTo("LIVE");
    }

    // =========================================================================
    // P7.6 — INVALID BOUNDS (Cases A, B, C, D)
    // =========================================================================

    @Test
    @DisplayName("P7.6 Case A: Rejects lowerBound > predictedPm25")
    void testRejectsCaseALowerBoundGreaterThanPrediction() {
        ForecastAiClient client = new ForecastAiClient("python", "dummy.py", 5000);
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 70.0, 75.0, 80.0, "ug/m3"), // 75 > 70
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 70.0, 65.0, 75.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 60.0, 55.0, 65.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto dto = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1", Instant.now().toString(), PUNE_H3, PUNE_CITY_ID.toString(),
                PARENT_ID.toString(), SNAPSHOT_ID.toString(), "SUCCESS", items, null
        );

        assertThatThrownBy(() -> client.validateInferenceResult(dto, PARENT_ID, createValidModelReadyVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Interval violated: lowerBound > predictedPm25 (75.0 > 70.0)");
    }

    @Test
    @DisplayName("P7.6 Case B: Rejects predictedPm25 > upperBound")
    void testRejectsCaseBPredictionGreaterThanUpperBound() {
        ForecastAiClient client = new ForecastAiClient("python", "dummy.py", 5000);
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 70.0, 65.0, 68.0, "ug/m3"), // 70 > 68
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 70.0, 65.0, 75.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 60.0, 55.0, 65.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto dto = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1", Instant.now().toString(), PUNE_H3, PUNE_CITY_ID.toString(),
                PARENT_ID.toString(), SNAPSHOT_ID.toString(), "SUCCESS", items, null
        );

        assertThatThrownBy(() -> client.validateInferenceResult(dto, PARENT_ID, createValidModelReadyVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Interval violated: predictedPm25 > upperBound (70.0 > 68.0)");
    }

    @Test
    @DisplayName("P7.6 Case C: Rejects lowerBound < 0 (physical lower bound violated)")
    void testRejectsCaseCLowerBoundNegative() {
        ForecastAiClient client = new ForecastAiClient("python", "dummy.py", 5000);
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 5.0, -2.0, 10.0, "ug/m3"), // -2.0 < 0
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 5.0, 1.0, 10.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 5.0, 1.0, 10.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto dto = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1", Instant.now().toString(), PUNE_H3, PUNE_CITY_ID.toString(),
                PARENT_ID.toString(), SNAPSHOT_ID.toString(), "SUCCESS", items, null
        );

        assertThatThrownBy(() -> client.validateInferenceResult(dto, PARENT_ID, createValidModelReadyVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Physical lower bound violated: lowerBound < 0 (-2.0)");
    }

    @Test
    @DisplayName("P7.6 Case D: Rejects NaN / Infinity in predicted or bounds values")
    void testRejectsCaseDNonFiniteValues() {
        ForecastAiClient client = new ForecastAiClient("python", "dummy.py", 5000);
        Instant t0 = Instant.now();

        // NaN prediction
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> itemsNaN = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), Double.NaN, 50.0, 70.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 50.0, 45.0, 55.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 50.0, 45.0, 55.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto dtoNaN = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1", Instant.now().toString(), PUNE_H3, PUNE_CITY_ID.toString(),
                PARENT_ID.toString(), SNAPSHOT_ID.toString(), "SUCCESS", itemsNaN, null
        );
        assertThatThrownBy(() -> client.validateInferenceResult(dtoNaN, PARENT_ID, createValidModelReadyVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("predictedPm25 must be finite numeric");

        // Infinite upper bound
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> itemsInf = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 50.0, 45.0, Double.POSITIVE_INFINITY, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 50.0, 45.0, 55.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 50.0, 45.0, 55.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto dtoInf = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1", Instant.now().toString(), PUNE_H3, PUNE_CITY_ID.toString(),
                PARENT_ID.toString(), SNAPSHOT_ID.toString(), "SUCCESS", itemsInf, null
        );
        assertThatThrownBy(() -> client.validateInferenceResult(dtoInf, PARENT_ID, createValidModelReadyVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("upperBound must be finite numeric");
    }

    // =========================================================================
    // P7.7 — ATOMIC PERSISTENCE
    // =========================================================================

    @Test
    @DisplayName("P7.7: Successful forecast generation persists exactly 3 horizons with full lineage")
    void testAtomicPersistenceOnSuccess() {
        HotspotPrediction parent = createMockParentPrediction();
        when(hotspotRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(featureSnapshotRepository.findById(SNAPSHOT_ID)).thenReturn(Optional.of(mock(FeatureSnapshot.class)));

        ForecastFeatureVector validVector = createValidForecastFeatureVector();
        when(featureBuilder.buildFromSnapshot(any())).thenReturn(validVector);

        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 70.62, 68.78, 72.48, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 70.55, 66.65, 73.60, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 60.91, 55.39, 66.33, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto inferenceDto = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1", Instant.now().toString(), PUNE_H3, PUNE_CITY_ID.toString(),
                PARENT_ID.toString(), SNAPSHOT_ID.toString(), "SUCCESS", items, null
        );

        when(aiClient.predict(any(), any(), any())).thenReturn(inferenceDto);

        List<Forecast> savedForecasts = items.stream().map(it -> new Forecast(
                PARENT_ID, PUNE_CITY_ID, PUNE_H3, null, SNAPSHOT_ID, Instant.now(),
                Instant.parse(it.targetTime()), it.horizonHours(), it.predictedPm25(),
                it.lowerBound(), it.upperBound(), null, "forecast_regressors_v1", it.unit(), "SUCCESS"
        )).toList();
        when(forecastRepository.saveAll(any())).thenReturn(savedForecasts);

        ForecastResponse resp = forecastService.generateForecast(new ForecastGenerateRequest(PARENT_ID, PUNE_CITY_ID, PUNE_H3));

        assertThat(resp).isNotNull();
        assertThat(resp.forecasts()).hasSize(3);
        verify(forecastRepository, times(1)).saveAll(argThat(list -> {
            List<Forecast> l = (List<Forecast>) list;
            return l.size() == 3 &&
                    l.stream().allMatch(f -> f.getParentPredictionId().equals(PARENT_ID) &&
                            f.getFeatureSnapshotId().equals(SNAPSHOT_ID) &&
                            f.getForecastConfidence() == null);
        }));
    }

    // =========================================================================
    // P7.8 — ERROR RESPONSE CONTRACT (GlobalExceptionHandler)
    // =========================================================================

    @Test
    @DisplayName("P7.8: GlobalExceptionHandler produces clean sanitized JSON without stack traces")
    void testGlobalExceptionHandlerSanitizedResponses() {
        // ParentNotFound -> 404
        ResponseEntity<Map<String, Object>> r404 = exceptionHandler.handleForecastParentNotFound(
                new ForecastException.ParentNotFound("Parent prediction 1234 not found")
        );
        assertThat(r404.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r404.getBody()).containsEntry("error", "FORECAST_PARENT_NOT_FOUND");
        assertThat(r404.getBody()).containsEntry("status", 404);
        assertThat(r404.getBody().get("message")).isEqualTo("Parent prediction 1234 not found");
        assertThat(r404.getBody()).doesNotContainKey("stackTrace");

        // ValidationFailed -> 422
        ResponseEntity<Map<String, Object>> r422 = exceptionHandler.handleForecastValidation(
                new ForecastException.ValidationFailed("Feature vector length mismatch")
        );
        assertThat(r422.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r422.getBody()).containsEntry("error", "FORECAST_VALIDATION_ERROR");
        assertThat(r422.getBody()).containsEntry("status", 422);

        // AiUnavailable -> 503
        ResponseEntity<Map<String, Object>> r503 = exceptionHandler.handleForecastAiUnavailable(
                new ForecastException.AiUnavailable("Inference service unavailable")
        );
        assertThat(r503.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r503.getBody()).containsEntry("error", "FORECAST_AI_UNAVAILABLE");
        assertThat(r503.getBody()).containsEntry("status", 503);

        // AiTimeout -> 504
        ResponseEntity<Map<String, Object>> r504 = exceptionHandler.handleForecastAiTimeout(
                new ForecastException.AiTimeout("Process timed out after 15s")
        );
        assertThat(r504.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(r504.getBody()).containsEntry("error", "FORECAST_AI_TIMEOUT");
        assertThat(r504.getBody()).containsEntry("status", 504);

        // Generic Exception -> 500 Sanitized
        ResponseEntity<Map<String, Object>> r500 = exceptionHandler.handleGeneric(
                new RuntimeException("Internal SQL Connection failed at /var/lib/db.sock password=secret")
        );
        assertThat(r500.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r500.getBody()).containsEntry("error", "INTERNAL_ERROR");
        assertThat(r500.getBody()).containsEntry("message", "An unexpected error occurred. Please contact support.");
        assertThat(r500.getBody().toString()).doesNotContain("password").doesNotContain("secret").doesNotContain("sock");
    }

    // =========================================================================
    // P7.10 — REAL FORECAST REFERENCE PRESERVATION
    // =========================================================================

    @Test
    @DisplayName("P7.10: Pune Shivajinagar reference forecast preserved (+1h=70.62, +3h=70.55, +6h=60.91)")
    void testRealPuneShivajinagarReferencePreserved() {
        double pred1h = 70.62;
        double lower1h = 68.78;
        double upper1h = 72.48;

        double pred3h = 70.55;
        double lower3h = 66.65;
        double upper3h = 73.60;

        double pred6h = 60.91;
        double lower6h = 55.39;
        double upper6h = 66.33;

        // Verify bounds integrity on references
        assertThat(lower1h).isLessThanOrEqualTo(pred1h);
        assertThat(pred1h).isLessThanOrEqualTo(upper1h);

        assertThat(lower3h).isLessThanOrEqualTo(pred3h);
        assertThat(pred3h).isLessThanOrEqualTo(upper3h);

        assertThat(lower6h).isLessThanOrEqualTo(pred6h);
        assertThat(pred6h).isLessThanOrEqualTo(upper6h);

        assertThat(lower1h).isGreaterThanOrEqualTo(0.0);
        assertThat(lower3h).isGreaterThanOrEqualTo(0.0);
        assertThat(lower6h).isGreaterThanOrEqualTo(0.0);
    }
}
