package com.aerosentinel.feature;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RealFeatureGenerationIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(RealFeatureGenerationIntegrationTest.class);

    @Autowired
    private FeatureEngineeringService featureEngineeringService;

    @Autowired
    private FeatureSnapshotRepository featureSnapshotRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private GridRepository gridRepository;

    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID MUMBAI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    private static final UUID DELHI_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");

    // Real H3 Resolution 8 cells from database
    private static final String PUNE_SHIVAJINAGAR_H3 = "88608850e5fffff";
    private static final String PUNE_KATRAJ_H3 = "88608852c1fffff";
    private static final String MUMBAI_KURLA_H3 = "88608b56b3fffff";
    private static final String DELHI_RK_PURAM_H3 = "883da11505fffff";

    @Test
    @Order(1)
    @DisplayName("1. Real Pune H3 cell generates exact 36-feature vector and persists snapshot")
    void testRealPuneFeatureGeneration() {
        Instant obsTime = Instant.now();
        FeatureRecord record = featureEngineeringService.generateFeatureRecord(PUNE_CITY_ID, PUNE_SHIVAJINAGAR_H3, obsTime);

        assertThat(record).isNotNull();
        assertThat(record.cityId()).isEqualTo(PUNE_CITY_ID);
        assertThat(record.h3Index()).isEqualTo(PUNE_SHIVAJINAGAR_H3);
        assertThat(record.featureSchemaVersion()).isEqualTo(FeatureRecord.SCHEMA_VERSION);

        Map<String, Object> feats = record.features();
        assertThat(feats).hasSize(36);

        // Verify exact ordered names are all present
        for (String featName : FeatureRecord.ORDERED_FEATURE_NAMES) {
            assertThat(feats).containsKey(featName);
            Object val = feats.get(featName);
            assertThat(val).withFailMessage("Feature %s is null", featName).isNotNull();
            assertThat(val).isInstanceOfAny(Number.class, Double.class, Integer.class, Float.class);
        }

        // Verify key real values
        log.info("Generated Pune 36-Feature Vector: {}", feats);
        assertThat((Number) feats.get("latitude")).isNotNull();
        assertThat((Number) feats.get("longitude")).isNotNull();
        assertThat((Number) feats.get("temperature")).isNotNull();
        assertThat((Number) feats.get("humidity")).isNotNull();
        assertThat((Number) feats.get("wind_speed")).isNotNull();
        assertThat((Number) feats.get("wind_u")).isNotNull();
        assertThat((Number) feats.get("wind_v")).isNotNull();
        assertThat((Number) feats.get("pressure")).isNotNull();

        // Verify snapshot in database
        Optional<FeatureSnapshot> savedOpt = featureSnapshotRepository
                .findByH3IndexAndObservedAtAndFeatureSchemaVersion(PUNE_SHIVAJINAGAR_H3, obsTime, FeatureRecord.SCHEMA_VERSION);
        assertThat(savedOpt).isPresent();
        FeatureSnapshot saved = savedOpt.get();
        assertThat(saved.getH3Index()).isEqualTo(PUNE_SHIVAJINAGAR_H3);
        assertThat(saved.getFeatureSchemaVersion()).isEqualTo("f3-features-v1");
        assertThat(saved.getQualityStatus()).isIn("VALID", "MISSING");
    }

    @Test
    @Order(2)
    @DisplayName("2. Multi-city availability: Pune, Mumbai, Delhi generate structured features with explicit missingness")
    void testMultiCityFeatureAvailability() {
        Instant now = Instant.now();

        // Pune
        FeatureRecord puneRecord = featureEngineeringService.generateFeatureRecord(PUNE_CITY_ID, PUNE_KATRAJ_H3, now);
        assertThat(puneRecord.features()).hasSize(36);
        log.info("Pune Katraj quality: {}, missing: {}", puneRecord.qualityStatus(), puneRecord.missingFeatures());

        // Mumbai
        FeatureRecord mumbaiRecord = featureEngineeringService.generateFeatureRecord(MUMBAI_CITY_ID, MUMBAI_KURLA_H3, now);
        assertThat(mumbaiRecord.features()).hasSize(36);
        log.info("Mumbai Kurla quality: {}, missing: {}", mumbaiRecord.qualityStatus(), mumbaiRecord.missingFeatures());
        // Mumbai lacks co-pollutant sensors -> explicitly tracked as missing/unavailable, NEVER fake numbers
        assertThat(mumbaiRecord.qualityStatus()).isIn(FeatureQualityStatus.MISSING, FeatureQualityStatus.UNAVAILABLE);
        assertThat(mumbaiRecord.missingFeatures()).contains("pm10", "no2");

        // Delhi
        FeatureRecord delhiRecord = featureEngineeringService.generateFeatureRecord(DELHI_CITY_ID, DELHI_RK_PURAM_H3, now);
        assertThat(delhiRecord.features()).hasSize(36);
        log.info("Delhi RK Puram quality: {}, missing: {}", delhiRecord.qualityStatus(), delhiRecord.missingFeatures());
        // Delhi lacks co-pollutant sensors in current DB -> explicitly tracked
        assertThat(delhiRecord.qualityStatus()).isIn(FeatureQualityStatus.MISSING, FeatureQualityStatus.UNAVAILABLE);
        assertThat(delhiRecord.missingFeatures()).contains("pm10", "no2");
    }

    @Test
    @Order(3)
    @DisplayName("3. Database audit: Feature snapshots table contains persisted records")
    void testFeatureSnapshotPersistenceCount() {
        long count = featureSnapshotRepository.count();
        assertThat(count).isGreaterThan(0);
        log.info("Total feature_snapshots in PostgreSQL: {}", count);
    }
}
