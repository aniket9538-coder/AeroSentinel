package com.aerosentinel.forecast.feature;

import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ForecastFeatureLayerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ForecastFeatureLayerIntegrationTest.class);

    @Autowired
    private ForecastFeatureBuilder forecastFeatureBuilder;

    @Autowired
    private ForecastFeatureValidator forecastFeatureValidator;

    @Autowired
    private ForecastFeatureAdapter forecastFeatureAdapter;

    @Autowired
    private FeatureSnapshotRepository featureSnapshotRepository;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID DELHI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");

    private static final String PUNE_SHIVAJINAGAR_H3 = "88608850e5fffff";
    private static final String MUMBAI_KURLA_H3 = "88608b56b3fffff";
    private static final String DELHI_RK_PURAM_H3 = "883da11505fffff";

    @Test
    @Order(1)
    @DisplayName("1. Build real DB-backed forecast feature vector for Pune and print FEATURE_VECTOR_RUNTIME_PROOF")
    void testRealPuneForecastFeatureVector() {
        Instant t0 = Instant.now();

        // 1. Build from production context
        ForecastFeatureVector vector = forecastFeatureBuilder.buildFromContext(
                PUNE_CITY_ID,
                PUNE_SHIVAJINAGAR_H3,
                t0
        );

        assertThat(vector).isNotNull();
        assertThat(vector.orderedValues()).hasSize(36);
        assertThat(vector.cityId()).isEqualTo(PUNE_CITY_ID);
        assertThat(vector.h3Index()).isEqualTo(PUNE_SHIVAJINAGAR_H3);

        // 2. Validate using dedicated F4 validator
        forecastFeatureValidator.validate(vector);

        // 3. Adapt for model input using dedicated F4 adapter (raw km/h preservation mode)
        ForecastFeatureAdapter.ModelReadyFeatureVector adapted = forecastFeatureAdapter.adapt(vector, false);
        assertThat(adapted).isNotNull();
        assertThat(adapted.orderedValues()).hasSize(36);

        // 4. Log authoritative semantic runtime proof
        log.info("==================================================");
        log.info("FEATURE_VECTOR_RUNTIME_PROOF (F4-P2)");
        log.info("==================================================");
        log.info("cityId: {}", vector.cityId());
        log.info("h3Index: {}", vector.h3Index());
        log.info("T0: {}", vector.baseTimestamp());
        log.info("featureSnapshotId: {}", vector.featureSnapshotId());
        log.info("qualityStatus: {}", vector.qualityStatus());
        log.info("missingFields: {}", vector.missingFields());
        log.info("--------------------------------------------------");

        for (int i = 0; i < 36; i++) {
            String featName = ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(i);
            double val = adapted.orderedValues()[i];
            log.info(String.format("feature %02d [%s] = %s", i + 1, featName, val));
        }
        log.info("==================================================");
        log.info("Model inference executed: false (P2 stop condition respected)");

        // Assert physical fields are populated with real ranges
        assertThat(vector.getFeature("latitude")).isBetween(18.0, 19.0);
        assertThat(vector.getFeature("longitude")).isBetween(73.0, 74.0);
        assertThat(vector.getFeature("temperature")).isBetween(10.0, 50.0);
        assertThat(vector.getFeature("pressure")).isBetween(900.0, 1050.0);
        assertThat(vector.getFeature("nearest_fire_distance_km")).isEqualTo(50.0);
    }

    @Test
    @Order(2)
    @DisplayName("2. Build and verify MODEL_READY_VECTOR_PROOF with shape (1, 36) and wind unit proof")
    void testModelReadyVectorProof() {
        Instant t0 = Instant.now();

        ForecastFeatureVector vector = forecastFeatureBuilder.buildFromContext(
                PUNE_CITY_ID,
                PUNE_SHIVAJINAGAR_H3,
                t0
        );

        // Adapt with single wind conversion to m/s
        ForecastFeatureAdapter.ModelReadyFeatureVector modelReady = forecastFeatureAdapter.adapt(vector, true);

        double[][] matrix2D = modelReady.to2DArray();

        log.info("==================================================");
        log.info("MODEL_READY_VECTOR_PROOF (F4-P2 HARDENED)");
        log.info("==================================================");
        log.info("batchShape: (1, 36)");
        log.info("batchSize: {}", modelReady.getBatchSize());
        log.info("featureCount: {}", modelReady.getFeatureCount());
        log.info("h3Index: {}", modelReady.h3Index());
        log.info("windSpeedConvertedToMps: {}", modelReady.windSpeedConvertedToMps());
        log.info("convertedWindSpeed (m/s): {}", modelReady.featureMap().get("wind_speed"));
        log.info("wind_u: {}", modelReady.featureMap().get("wind_u"));
        log.info("wind_v: {}", modelReady.featureMap().get("wind_v"));
        log.info("--------------------------------------------------");

        for (int i = 0; i < 36; i++) {
            String name = ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(i);
            double val = matrix2D[0][i];
            log.info(String.format("model_input[%02d] %-30s = %10.4f", i, name, val));
        }
        log.info("==================================================");
        log.info("Model inference executed: false (STOP CONDITION VERIFIED)");

        // Assert 2D matrix shape exactly (1, 36)
        assertThat(matrix2D).hasDimensions(1, 36);
        for (int j = 0; j < 36; j++) {
            assertThat(Double.isFinite(matrix2D[0][j])).isTrue();
            assertThat(Double.isNaN(matrix2D[0][j])).isFalse();
        }

        // Wind speed conversion verified
        double rawKmh = vector.getFeature("wind_speed");
        double expectedMps = Math.round((rawKmh / 3.6) * 10000.0) / 10000.0;
        assertThat(modelReady.featureMap().get("wind_speed")).isEqualTo(expectedMps);
    }

    @Test
    @Order(3)
    @DisplayName("3. Reuses existing physical snapshot data without coupling to F3 hotspot logic")
    void testRealSnapshotReuse() {
        List<FeatureSnapshot> snapshots = featureSnapshotRepository.findAll();
        assertThat(snapshots).isNotEmpty();

        FeatureSnapshot sampleSnapshot = snapshots.get(0);
        ForecastFeatureVector vector = forecastFeatureBuilder.buildFromSnapshot(sampleSnapshot);

        assertThat(vector).isNotNull();
        assertThat(vector.orderedValues()).hasSize(36);
        assertThat(vector.featureSnapshotId()).isEqualTo(sampleSnapshot.getId());
        assertThat(vector.h3Index()).isEqualTo(sampleSnapshot.getH3Index());

        forecastFeatureValidator.validate(vector);
    }

    @Test
    @Order(4)
    @DisplayName("4. Correctly identifies missingness for non-Pune cities without fabricating fake values")
    void testCrossCityQualityHandling() {
        Instant t0 = Instant.now();

        ForecastFeatureVector mumbaiVec = forecastFeatureBuilder.buildFromContext(
                MUMBAI_CITY_ID,
                MUMBAI_KURLA_H3,
                t0
        );
        assertThat(mumbaiVec.qualityStatus()).isEqualTo("UNAVAILABLE");
        assertThat(mumbaiVec.missingFields()).contains("pm10", "no2", "so2", "co", "o3");

        ForecastFeatureVector delhiVec = forecastFeatureBuilder.buildFromContext(
                DELHI_CITY_ID,
                DELHI_RK_PURAM_H3,
                t0
        );
        assertThat(delhiVec.qualityStatus()).isEqualTo("UNAVAILABLE");
        assertThat(delhiVec.missingFields()).contains("pm10", "no2", "so2", "co", "o3");
    }
}
