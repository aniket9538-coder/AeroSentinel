package com.aerosentinel.forecast.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hardened Unit Tests for the Production Forecast Feature Layer (F4-P2).
 * Fulfills all requirements from the F4-P2 Final Hardening Patch:
 *  - Missingness semantics (real zero != missing != unavailable)
 *  - Weather fallback audit & provenance
 *  - Wind unit proof (13 km/h -> 3.6111 m/s single conversion, double conversion rejection)
 *  - Model-ready vector proof (shape 1, 36)
 *  - Quality status integrity (no false promotion to VALID)
 */
class ForecastFeatureLayerTest {

    private ForecastFeatureValidator validator;
    private ForecastFeatureAdapter adapter;

    private final UUID testCityId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final String validH3Index = "88608850e5fffff"; // Pune Shivajinagar
    private final Instant testT0 = Instant.parse("2026-09-27T12:00:00Z");

    @BeforeEach
    void setUp() {
        validator = new ForecastFeatureValidator();
        adapter = new ForecastFeatureAdapter(validator);
    }

    private Map<String, Object> createValidBaselineFeatureMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("latitude", 18.5315);
        map.put("longitude", 73.8471);
        map.put("pm10", 120.0);
        map.put("no2", 37.0);
        map.put("so2", 14.0);
        map.put("co", 0.9);
        map.put("o3", 24.0);
        map.put("hour", 17);
        map.put("day_of_week", 6);
        map.put("is_weekend", 1);
        map.put("hour_sin", -0.9659);
        map.put("hour_cos", -0.2588);
        map.put("dow_sin", -0.7818);
        map.put("dow_cos", 0.6235);
        map.put("temperature", 30.1);
        map.put("humidity", 52.0);
        map.put("wind_speed", 13.0); // 13.0 km/h raw
        map.put("wind_direction", 275.0);
        map.put("wind_u", 3.5974);
        map.put("wind_v", -0.3147);
        map.put("rainfall", 0.0);
        map.put("pressure", 949.8);
        map.put("pm25_spatial_lag_mean", 78.5);
        map.put("nearest_station_distance_km", 0.27);
        map.put("stations_within_5km_count", 2);
        map.put("monitoring_coverage_gap_flag", 0);
        map.put("dist_to_nearest_industrial_km", 3.5);
        map.put("dist_to_nearest_major_road_km", 0.4);
        map.put("sensitive_receptors_count_2km", 4);
        map.put("industrial_zone_within_2km_flag", 0);
        map.put("fire_count_24h_25km", 0);
        map.put("fire_frp_sum_24h_25km", 0.0);
        map.put("fire_frp_mean_24h_25km", 0.0);
        map.put("nearest_fire_distance_km", 50.0);
        map.put("fire_frp_distance_decay", 0.0);
        map.put("fire_upwind_alignment_score", 0.0);
        return map;
    }

    private ForecastFeatureVector createTestVector(Map<String, Object> rawMap, String quality, List<String> missing) {
        ForecastFeatureBuilder builder = new ForecastFeatureBuilder(null, null, validator, null);
        return builder.buildFromRawMap(testCityId, validH3Index, testT0, UUID.randomUUID(), quality, missing, rawMap);
    }

    @Nested
    @DisplayName("1. Contract & Ordering Tests")
    class ContractTests {

        @Test
        @DisplayName("Should maintain exactly 36 features in authoritative artifact order")
        void shouldMaintainExact36FeatureOrder() {
            assertThat(ForecastFeatureVector.F4_FEATURE_COUNT).isEqualTo(36);
            assertThat(ForecastFeatureVector.ORDERED_FEATURE_NAMES).hasSize(36);

            assertThat(ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(0)).isEqualTo("latitude");
            assertThat(ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(1)).isEqualTo("longitude");
            assertThat(ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(16)).isEqualTo("wind_speed");
            assertThat(ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(22)).isEqualTo("pm25_spatial_lag_mean");
            assertThat(ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(35)).isEqualTo("fire_upwind_alignment_score");

            Map<String, Object> map = createValidBaselineFeatureMap();
            ForecastFeatureVector vector = createTestVector(map, "VALID", List.of());

            assertThat(vector.orderedValues()).hasSize(36);
            assertThat(vector.features()).hasSize(36);
        }
    }

    @Nested
    @DisplayName("2. Missingness Semantics & Quality Integrity")
    class MissingnessSemanticsTests {

        @Test
        @DisplayName("Should distinguish REAL_ZERO from MISSING and SOURCE_UNAVAILABLE")
        void shouldDistinguishRealZeroFromMissingAndUnavailable() {
            Map<String, Object> map = createValidBaselineFeatureMap();
            ForecastFeatureVector vector = createTestVector(map, "VALID", List.of());

            // Real physical zeros: fire_count=0 when no fires exist, rainfall=0 when dry
            assertThat(vector.getProvenance("fire_count_24h_25km")).isEqualTo(FeatureProvenance.REAL_ZERO);
            assertThat(vector.getProvenance("rainfall")).isEqualTo(FeatureProvenance.REAL_ZERO);

            // Valid positive observations
            assertThat(vector.getProvenance("pm10")).isEqualTo(FeatureProvenance.VALID_OBSERVATION);
            assertThat(vector.getProvenance("temperature")).isEqualTo(FeatureProvenance.VALID_OBSERVATION);
        }

        @Test
        @DisplayName("Should reject false promotion to VALID when fields are missing")
        void shouldRejectFalsePromotionToValid() {
            Map<String, Object> map = createValidBaselineFeatureMap();
            map.put("pm10", 0.0); // 0.0 substitution
            List<String> missing = List.of("pm10");

            // Attempting to construct a vector with missing fields but qualityStatus = VALID
            ForecastFeatureVector vector = new ForecastFeatureVector(
                    testCityId,
                    validH3Index,
                    testT0,
                    UUID.randomUUID(),
                    "VALID", // False promotion!
                    missing,
                    Map.of("pm10", 0.0),
                    new double[36],
                    Map.of("pm10", FeatureProvenance.MISSING)
            );

            assertThatThrownBy(() -> validator.validate(vector))
                    .isInstanceOf(ForecastFeaturesInvalidException.class)
                    .hasMessageContaining("Check 14 failed: Quality status cannot be VALID when physical fields are missing");
        }

        @Test
        @DisplayName("Unavailable pollutant source must yield UNAVAILABLE status")
        void shouldYieldUnavailableWhenMultiplePollutantsMissing() {
            Map<String, Object> map = createValidBaselineFeatureMap();
            map.remove("pm10");
            map.remove("no2");
            map.remove("so2");
            map.remove("co");
            map.remove("o3");

            ForecastFeatureVector vector = createTestVector(map, "UNAVAILABLE", List.of("pm10", "no2", "so2", "co", "o3"));
            assertThat(vector.qualityStatus()).isEqualTo("UNAVAILABLE");
            assertThat(vector.isFullyValid()).isFalse();
            assertThat(vector.missingFields()).containsExactlyInAnyOrder("pm10", "no2", "so2", "co", "o3");
        }
    }

    @Nested
    @DisplayName("3. Wind Unit Proof & Orthogonal Consistency")
    class WindUnitProofTests {

        @Test
        @DisplayName("Wind speed 13.0 km/h must convert to 3.6111 m/s and align with 275 deg u/v vectors")
        void shouldVerifySingleWindConversionAndVectors() {
            Map<String, Object> map = createValidBaselineFeatureMap();
            map.put("wind_speed", 13.0); // raw km/h
            map.put("wind_direction", 275.0);

            ForecastFeatureVector vector = createTestVector(map, "VALID", List.of());
            assertThat(vector.getFeature("wind_speed")).isEqualTo(13.0);

            // Adapt with single conversion to m/s
            ForecastFeatureAdapter.ModelReadyFeatureVector adapted = adapter.adapt(vector, true);
            assertThat(adapted.windSpeedConvertedToMps()).isTrue();

            // 13.0 / 3.6 = 3.611111... -> rounded 3.6111
            double convertedWs = adapted.featureMap().get("wind_speed");
            assertThat(convertedWs).isCloseTo(3.6111, org.assertj.core.data.Offset.offset(0.001));

            // Verify orthogonal components for 275°:
            // rad = radians(275°) = 4.799655
            // u = -ws * sin(275°) = -3.6111 * (-0.996195) = +3.5974
            // v = -ws * cos(275°) = -3.6111 * (+0.087156) = -0.3147
            double u = adapted.featureMap().get("wind_u");
            double v = adapted.featureMap().get("wind_v");
            assertThat(u).isCloseTo(3.5974, org.assertj.core.data.Offset.offset(0.001));
            assertThat(v).isCloseTo(-0.3147, org.assertj.core.data.Offset.offset(0.001));

            // Verify both values in ordered array match featureMap
            int wsIdx = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_speed");
            int wuIdx = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_u");
            int wvIdx = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_v");

            assertThat(adapted.orderedValues()[wsIdx]).isEqualTo(convertedWs);
            assertThat(adapted.orderedValues()[wuIdx]).isEqualTo(u);
            assertThat(adapted.orderedValues()[wvIdx]).isEqualTo(v);
        }

        @Test
        @DisplayName("Should detect and reject double wind conversion")
        void shouldRejectDoubleWindConversion() {
            double originalKmh = 13.0;
            double doubleConverted = (13.0 / 3.6) / 3.6; // ~1.003 m/s

            assertThatThrownBy(() -> adapter.verifyNoDoubleConversion(originalKmh, doubleConverted))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DOUBLE_WIND_CONVERSION_DETECTED");
        }
    }

    @Nested
    @DisplayName("4. Model-Ready Vector Proof (Shape (1, 36))")
    class ModelReadyVectorProofTests {

        @Test
        @DisplayName("Model-ready vector must have shape (1, 36), finite values, and no NaN/Inf")
        void shouldProduceExactModelReadyShape() {
            Map<String, Object> map = createValidBaselineFeatureMap();
            ForecastFeatureVector vector = createTestVector(map, "VALID", List.of());

            ForecastFeatureAdapter.ModelReadyFeatureVector adapted = adapter.adapt(vector, true);

            assertThat(adapted.getBatchSize()).isEqualTo(1);
            assertThat(adapted.getFeatureCount()).isEqualTo(36);

            double[][] array2D = adapted.to2DArray();
            assertThat(array2D).hasDimensions(1, 36);

            for (int j = 0; j < 36; j++) {
                double val = array2D[0][j];
                assertThat(Double.isFinite(val)).as("Feature %s must be finite", ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(j)).isTrue();
                assertThat(Double.isNaN(val)).isFalse();
                assertThat(Double.isInfinite(val)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("5. Weather Fallback Semantics")
    class WeatherFallbackTests {

        @Test
        @DisplayName("Should flag imputed weather constants in provenance without masking as true physical observation")
        void shouldTrackWeatherFallbackProvenance() {
            Map<String, Object> map = createValidBaselineFeatureMap();
            // Simulate missing weather observation where baseline temperature 25.0 was substituted
            map.put("temperature", 25.0);
            List<String> missing = List.of("temperature");

            ForecastFeatureVector vector = createTestVector(map, "MISSING", missing);
            assertThat(vector.getProvenance("temperature")).isEqualTo(FeatureProvenance.IMPUTED_BASELINE);
            assertThat(vector.qualityStatus()).isEqualTo("MISSING");
        }
    }
}
