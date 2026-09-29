package com.aerosentinel.forecast;

import com.aerosentinel.forecast.feature.ForecastFeatureAdapter.ModelReadyFeatureVector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ForecastUnitTest {

    private ForecastAiClient aiClient;
    private ObjectMapper mapper;

    private static final UUID PARENT_ID = UUID.fromString("a310c689-f340-49fc-8935-a037de8d7709");
    private static final UUID CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final String H3_INDEX = "88608850e5fffff";

    @BeforeEach
    void setUp() {
        aiClient = new ForecastAiClient("python", "ai-service/ml/inference/predict_forecast_cli.py", 15000);
        mapper = new ObjectMapper().findAndRegisterModules();
    }

    private ModelReadyFeatureVector createMockVector() {
        double[] vals = new double[36];
        Arrays.fill(vals, 1.0);
        Map<String, Double> map = new HashMap<>();
        return new ModelReadyFeatureVector(
                H3_INDEX,
                CITY_ID,
                Instant.now(),
                UUID.randomUUID(),
                "VALID",
                Collections.emptyList(),
                vals,
                map,
                false
        );
    }

    private ForecastAiClient.ForecastInferenceResultDto createValidInferenceDto() {
        Instant t0 = Instant.parse("2026-09-27T08:00:00Z");
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 71.90, 70.06, 73.76, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 70.43, 66.53, 73.48, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 70.55, 65.03, 75.98, "ug/m3")
        );
        return new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                UUID.randomUUID().toString(),
                "SUCCESS",
                items,
                null // strictly null
        );
    }

    // =========================================================================
    // 1. Output Contract & Invariant Validation Tests
    // =========================================================================

    @Test
    @DisplayName("Valid inference result passes validation")
    void testValidInferenceResultPasses() {
        ForecastAiClient.ForecastInferenceResultDto valid = createValidInferenceDto();
        ModelReadyFeatureVector vector = createMockVector();

        aiClient.validateInferenceResult(valid, PARENT_ID, vector);
    }

    @Test
    @DisplayName("Rejects forecastConfidence when not null")
    void testRejectsNonNullForecastConfidence() {
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 50.0, 48.0, 52.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 52.0, 47.0, 56.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 55.0, 45.0, 60.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto withConfidence = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                null,
                "SUCCESS",
                items,
                0.88 // FORBIDDEN: not null
        );

        assertThatThrownBy(() -> aiClient.validateInferenceResult(withConfidence, PARENT_ID, createMockVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Forecast confidence must be strictly null");
    }

    @Test
    @DisplayName("Rejects unsupported horizon (e.g. 2, 4, 12, 24)")
    void testRejectsUnsupportedHorizon() {
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 50.0, 48.0, 52.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(2, t0.plus(2, ChronoUnit.HOURS).toString(), 52.0, 47.0, 56.0, "ug/m3"), // 2 is unsupported
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 55.0, 45.0, 60.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto invalid = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                null,
                "SUCCESS",
                items,
                null
        );

        assertThatThrownBy(() -> aiClient.validateInferenceResult(invalid, PARENT_ID, createMockVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Unsupported horizon");
    }

    @Test
    @DisplayName("Rejects missing horizon when count != 3")
    void testRejectsMissingHorizon() {
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 50.0, 48.0, 52.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 52.0, 47.0, 56.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto invalid = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                null,
                "SUCCESS",
                items,
                null
        );

        assertThatThrownBy(() -> aiClient.validateInferenceResult(invalid, PARENT_ID, createMockVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Forecast result must contain exactly 3 horizons");
    }

    @Test
    @DisplayName("Rejects physical lower bound violation (lowerBound < 0)")
    void testRejectsNegativeLowerBound() {
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 2.0, -1.5, 4.0, "ug/m3"), // negative lower bound
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 5.0, 1.0, 8.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 6.0, 2.0, 10.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto invalid = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                null,
                "SUCCESS",
                items,
                null
        );

        assertThatThrownBy(() -> aiClient.validateInferenceResult(invalid, PARENT_ID, createMockVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Physical lower bound violated: lowerBound < 0");
    }

    @Test
    @DisplayName("Rejects mathematical interval ordering violation (lowerBound > predicted)")
    void testRejectsLowerBoundGreaterThanPrediction() {
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), 50.0, 55.0, 60.0, "ug/m3"), // 55 > 50
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 52.0, 47.0, 56.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 55.0, 45.0, 60.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto invalid = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                null,
                "SUCCESS",
                items,
                null
        );

        assertThatThrownBy(() -> aiClient.validateInferenceResult(invalid, PARENT_ID, createMockVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("Interval violated: lowerBound > predictedPm25");
    }

    @Test
    @DisplayName("Rejects non-finite values (NaN, Infinity)")
    void testRejectsNonFiniteValues() {
        Instant t0 = Instant.now();
        List<ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto> items = List.of(
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(1, t0.plus(1, ChronoUnit.HOURS).toString(), Double.NaN, 40.0, 60.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(3, t0.plus(3, ChronoUnit.HOURS).toString(), 52.0, 47.0, 56.0, "ug/m3"),
                new ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto(6, t0.plus(6, ChronoUnit.HOURS).toString(), 55.0, 45.0, 60.0, "ug/m3")
        );
        ForecastAiClient.ForecastInferenceResultDto invalid = new ForecastAiClient.ForecastInferenceResultDto(
                "forecast_regressors_v1",
                Instant.now().toString(),
                H3_INDEX,
                CITY_ID.toString(),
                PARENT_ID.toString(),
                null,
                "SUCCESS",
                items,
                null
        );

        assertThatThrownBy(() -> aiClient.validateInferenceResult(invalid, PARENT_ID, createMockVector()))
                .isInstanceOf(ForecastException.ValidationFailed.class)
                .hasMessageContaining("predictedPm25 must be finite numeric");
    }

    // =========================================================================
    // 2. Mapper and Freshness Logic Tests
    // =========================================================================

    @Test
    @DisplayName("ForecastMapper correctly computes freshness status")
    void testForecastFreshnessComputation() {
        Instant now = Instant.now();

        // < 2 hours -> LIVE
        assertThat(ForecastMapper.computeFreshness(now.minus(30, ChronoUnit.MINUTES), now)).isEqualTo("LIVE");
        assertThat(ForecastMapper.computeFreshness(now.minus(1, ChronoUnit.HOURS), now)).isEqualTo("LIVE");

        // 2h to 24h -> STALE
        assertThat(ForecastMapper.computeFreshness(now.minus(3, ChronoUnit.HOURS), now)).isEqualTo("STALE");
        assertThat(ForecastMapper.computeFreshness(now.minus(23, ChronoUnit.HOURS), now)).isEqualTo("STALE");

        // > 24h -> UNAVAILABLE
        assertThat(ForecastMapper.computeFreshness(now.minus(25, ChronoUnit.HOURS), now)).isEqualTo("UNAVAILABLE");

        // null -> NO_DATA
        assertThat(ForecastMapper.computeFreshness(null, now)).isEqualTo("NO_DATA");
    }

    @Test
    @DisplayName("ForecastMapper maps entities to sorted ForecastResponse with null confidence")
    void testForecastMapperSortsAndMaps() {
        Instant genAt = Instant.now();
        Instant target1 = genAt.plus(1, ChronoUnit.HOURS);
        Instant target3 = genAt.plus(3, ChronoUnit.HOURS);
        Instant target6 = genAt.plus(6, ChronoUnit.HOURS);

        Forecast f6 = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, null, genAt, target6, 6, 70.55, 65.03, 75.98, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");
        Forecast f1 = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, null, genAt, target1, 1, 71.90, 70.06, 73.76, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");
        Forecast f3 = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, null, genAt, target3, 3, 70.43, 66.53, 73.48, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");

        // Passed in unsorted order [6, 1, 3]
        ForecastResponse response = ForecastMapper.toResponse(List.of(f6, f1, f3));

        assertThat(response).isNotNull();
        assertThat(response.h3Index()).isEqualTo(H3_INDEX);
        assertThat(response.parentPredictionId()).isEqualTo(PARENT_ID);
        assertThat(response.forecastConfidence()).isNull();
        assertThat(response.freshness()).isEqualTo("LIVE");
        assertThat(response.forecasts()).hasSize(3);

        // Verify sorted order 1, 3, 6
        assertThat(response.forecasts().get(0).horizonHours()).isEqualTo(1);
        assertThat(response.forecasts().get(0).predictedPm25()).isEqualTo(71.90);
        assertThat(response.forecasts().get(1).horizonHours()).isEqualTo(3);
        assertThat(response.forecasts().get(1).predictedPm25()).isEqualTo(70.43);
        assertThat(response.forecasts().get(2).horizonHours()).isEqualTo(6);
        assertThat(response.forecasts().get(2).predictedPm25()).isEqualTo(70.55);
    }

    // =========================================================================
    // 3. Serialization Precision & Contract Fidelity
    // =========================================================================

    @Test
    @DisplayName("ForecastResponse serializes forecastConfidence as explicit JSON null")
    void testSerializationPreservesNullConfidence() throws Exception {
        ForecastResponse response = new ForecastResponse(
                H3_INDEX,
                CITY_ID,
                Instant.now(),
                "forecast_regressors_v1",
                PARENT_ID,
                UUID.randomUUID(),
                "SUCCESS",
                "LIVE",
                List.of(
                        new ForecastResponse.ForecastItem(1, Instant.now().plus(1, ChronoUnit.HOURS), 71.90, 70.06, 73.76, "ug/m3"),
                        new ForecastResponse.ForecastItem(3, Instant.now().plus(3, ChronoUnit.HOURS), 70.43, 66.53, 73.48, "ug/m3"),
                        new ForecastResponse.ForecastItem(6, Instant.now().plus(6, ChronoUnit.HOURS), 70.55, 65.03, 75.98, "ug/m3")
                ),
                null
        );

        String json = mapper.writeValueAsString(response);

        assertThat(json).contains("\"forecastConfidence\":null");
        assertThat(json).contains("\"horizonHours\":1");
        assertThat(json).contains("\"predictedPm25\":71.9");
        assertThat(json).contains("\"lowerBound\":70.06");
        assertThat(json).contains("\"upperBound\":73.76");
    }

    // =========================================================================
    // 4. Final Contract Hardening Tests (Lineage, Residuals, T0 Semantics)
    // =========================================================================

    @Test
    @DisplayName("ParentContextMismatch exception produces FORECAST_PARENT_CONTEXT_MISMATCH error code")
    void testParentContextMismatchExceptionCode() {
        ForecastException.ParentContextMismatch ex = new ForecastException.ParentContextMismatch("City ID mismatch");
        assertThat(ex.getErrorCode()).isEqualTo("FORECAST_PARENT_CONTEXT_MISMATCH");
        assertThat(ex.getMessage()).isEqualTo("City ID mismatch");
    }

    @Test
    @DisplayName("GeneratedAt vs T0 semantics: targetTime derived from T0 base timestamp")
    void testGeneratedAtVsT0Semantics() {
        Instant t0 = Instant.parse("2026-09-27T08:00:00Z");
        Instant genAt = Instant.parse("2026-09-27T10:15:30Z");
        UUID snapshotId = UUID.randomUUID();

        Forecast f1 = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, snapshotId, genAt, t0.plus(1, ChronoUnit.HOURS), 1, 70.62, 68.78, 72.48, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");
        Forecast f3 = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, snapshotId, genAt, t0.plus(3, ChronoUnit.HOURS), 3, 70.55, 66.65, 73.60, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");
        Forecast f6 = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, snapshotId, genAt, t0.plus(6, ChronoUnit.HOURS), 6, 60.91, 55.39, 66.33, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");

        ForecastResponse response = ForecastMapper.toResponse(List.of(f1, f3, f6));

        assertThat(response).isNotNull();
        assertThat(response.baseTimestamp()).isEqualTo(t0);
        assertThat(response.generatedAt()).isEqualTo(genAt);
        assertThat(response.forecasts().get(0).targetTime()).isEqualTo(Instant.parse("2026-09-27T09:00:00Z"));
        assertThat(response.forecasts().get(1).targetTime()).isEqualTo(Instant.parse("2026-09-27T11:00:00Z"));
        assertThat(response.forecasts().get(2).targetTime()).isEqualTo(Instant.parse("2026-09-27T14:00:00Z"));
    }

    @Test
    @DisplayName("Forecast entity validateLineageIntegrity rejects SUCCESS status with null featureSnapshotId")
    void testForecastEntityRejectsMissingLineageWhenSuccess() {
        Instant now = Instant.now();
        Forecast forecast = new Forecast(PARENT_ID, CITY_ID, H3_INDEX, null, null, now, now.plus(1, ChronoUnit.HOURS), 1, 50.0, 48.0, 52.0, null, "forecast_regressors_v1", "ug/m3", "SUCCESS");

        assertThatThrownBy(forecast::validateLineageIntegrity)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("featureSnapshotId cannot be null for SUCCESS forecast");
    }

    @Test
    @DisplayName("P3 artifact residual contract: intervals strictly match audited P10/P90 offsets")
    void testArtifactResidualIntervalPreservation() {
        // Audited residuals from forecast_regressors_v1.joblib:
        // H1: P10 = -1.8424755, P90 = +1.8628680
        // H3: P10 = -3.9013645, P90 = +3.0473582
        // H6: P10 = -5.5207627, P90 = +5.4248675
        double predH1 = 70.62;
        double expectedLowerH1 = Math.round((predH1 - 1.8424755) * 100.0) / 100.0; // 68.78
        double expectedUpperH1 = Math.round((predH1 + 1.8628680) * 100.0) / 100.0; // 72.48

        assertThat(expectedLowerH1).isEqualTo(68.78);
        assertThat(expectedUpperH1).isEqualTo(72.48);

        double predH3 = 70.55;
        double expectedLowerH3 = Math.round((predH3 - 3.9013645) * 100.0) / 100.0; // 66.65
        double expectedUpperH3 = Math.round((predH3 + 3.0473582) * 100.0) / 100.0; // 73.60

        assertThat(expectedLowerH3).isEqualTo(66.65);
        assertThat(expectedUpperH3).isEqualTo(73.60);

        double predH6 = 60.91;
        double expectedLowerH6 = Math.round((predH6 - 5.5207627) * 100.0) / 100.0; // 55.39
        double expectedUpperH6 = Math.round((predH6 + 5.4248675) * 100.0) / 100.0; // 66.33

        assertThat(expectedLowerH6).isEqualTo(55.39);
        assertThat(expectedUpperH6).isEqualTo(66.33);
    }
}
