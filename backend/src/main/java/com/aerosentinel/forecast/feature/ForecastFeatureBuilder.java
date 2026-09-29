package com.aerosentinel.forecast.feature;

import com.aerosentinel.feature.FeatureEngineeringService;
import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Dedicated F4 Forecast Feature Builder with Hardened Missingness Semantics.
 *
 * Gathers physical production data across F2 air/weather observations, H3 spatial grid,
 * leave-one-out spatial lag, monitoring stations, GIS features, and NASA FIRMS fire features.
 *
 * Enforces explicit provenance tracking:
 *   - VALID_OBSERVATION: True physical telemetry
 *   - REAL_ZERO: Confirmed physical zero (e.g. 0 active fires)
 *   - MISSING: Missing telemetry sensor value
 *   - SOURCE_UNAVAILABLE: External data source offline
 *   - IMPUTED_BASELINE: Model baseline substitution (never promoted to VALID)
 */
@Service
public class ForecastFeatureBuilder {

    private static final Logger log = LoggerFactory.getLogger(ForecastFeatureBuilder.class);

    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final FeatureEngineeringService featureEngineeringService;
    private final ForecastFeatureValidator validator;
    private final ObjectMapper objectMapper;

    public ForecastFeatureBuilder(
            FeatureSnapshotRepository featureSnapshotRepository,
            FeatureEngineeringService featureEngineeringService,
            ForecastFeatureValidator validator,
            ObjectMapper objectMapper) {
        this.featureSnapshotRepository = featureSnapshotRepository;
        this.featureEngineeringService = featureEngineeringService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a ForecastFeatureVector for the given city, H3 cell, and base timestamp T0.
     * Leverages existing production snapshots if present, or requests physical feature generation.
     */
    @Transactional
    public ForecastFeatureVector buildFromContext(UUID cityId, String h3Index, Instant baseTimestamp) {
        if (cityId == null) {
            throw new IllegalArgumentException("cityId must not be null");
        }
        if (h3Index == null || h3Index.isBlank()) {
            throw new IllegalArgumentException("h3Index must not be null or blank");
        }
        if (baseTimestamp == null) {
            throw new IllegalArgumentException("baseTimestamp must not be null");
        }

        // 1. Look for existing physical snapshot at T0
        Optional<FeatureSnapshot> existing = featureSnapshotRepository
                .findByH3IndexAndObservedAtAndFeatureSchemaVersion(h3Index, baseTimestamp, FeatureRecord.SCHEMA_VERSION);

        if (existing.isPresent()) {
            return buildFromSnapshot(existing.get());
        }

        // 2. Generate physical record through production feature infrastructure
        FeatureRecord record = featureEngineeringService.generateFeatureRecord(cityId, h3Index, baseTimestamp);

        // Retrieve the newly persisted snapshot to get its UUID
        Optional<FeatureSnapshot> persisted = featureSnapshotRepository
                .findByH3IndexAndObservedAtAndFeatureSchemaVersion(h3Index, baseTimestamp, FeatureRecord.SCHEMA_VERSION);

        UUID snapshotId = persisted.map(FeatureSnapshot::getId).orElse(null);

        return buildFromRawMap(
                cityId,
                h3Index,
                baseTimestamp,
                snapshotId,
                record.qualityStatus().name(),
                record.missingFeatures(),
                record.features()
        );
    }

    /**
     * Builds a ForecastFeatureVector directly from an existing physical FeatureSnapshot.
     */
    public ForecastFeatureVector buildFromSnapshot(FeatureSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("FeatureSnapshot must not be null");
        }

        Map<String, Object> rawMap;
        try {
            rawMap = objectMapper.readValue(snapshot.getFeatures(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new ForecastFeaturesInvalidException(
                    "Failed to parse snapshot JSON features: " + e.getMessage(),
                    snapshot.getH3Index()
            );
        }

        List<String> missing = snapshot.getMissingFeatures() != null
                ? Arrays.asList(snapshot.getMissingFeatures())
                : Collections.emptyList();

        return buildFromRawMap(
                snapshot.getCityId(),
                snapshot.getH3Index(),
                snapshot.getObservedAt(),
                snapshot.getId(),
                snapshot.getQualityStatus(),
                missing,
                rawMap
        );
    }

    /**
     * Internal builder converting raw map to ForecastFeatureVector, establishing
     * field-by-field provenance, and validating quality status integrity.
     */
    public ForecastFeatureVector buildFromRawMap(
            UUID cityId,
            String h3Index,
            Instant baseTimestamp,
            UUID snapshotId,
            String qualityStatus,
            List<String> missingFields,
            Map<String, Object> rawMap) {

        double[] orderedValues = new double[ForecastFeatureVector.F4_FEATURE_COUNT];
        Map<String, Double> verifiedMap = new LinkedHashMap<>();
        Map<String, FeatureProvenance> provenanceMap = new LinkedHashMap<>();
        List<String> detectedMissing = new ArrayList<>(missingFields != null ? missingFields : Collections.emptyList());

        for (int i = 0; i < ForecastFeatureVector.F4_FEATURE_COUNT; i++) {
            String featName = ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(i);
            Object valObj = rawMap != null ? rawMap.get(featName) : null;

            if (valObj == null) {
                if (!detectedMissing.contains(featName)) {
                    detectedMissing.add(featName);
                }
                orderedValues[i] = 0.0;
                verifiedMap.put(featName, 0.0);
                provenanceMap.put(featName, FeatureProvenance.MISSING);
            } else if (valObj instanceof Number num) {
                double d = num.doubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw new ForecastFeaturesInvalidException(
                            "Feature '" + featName + "' contains non-finite value: " + d,
                            h3Index
                    );
                }
                orderedValues[i] = d;
                verifiedMap.put(featName, d);

                // Differentiate real physical zero from positive observation or imputed baseline
                if (detectedMissing.contains(featName)) {
                    provenanceMap.put(featName, FeatureProvenance.IMPUTED_BASELINE);
                } else if (d == 0.0 && isLegitimatePhysicalZeroFeature(featName)) {
                    provenanceMap.put(featName, FeatureProvenance.REAL_ZERO);
                } else {
                    provenanceMap.put(featName, FeatureProvenance.VALID_OBSERVATION);
                }
            } else {
                throw new ForecastFeaturesInvalidException(
                        "Feature '" + featName + "' is not numeric: " + valObj,
                        h3Index
                );
            }
        }

        // Quality status integrity check: do not falsely promote missing/imputed records to VALID
        String finalQuality = qualityStatus != null ? qualityStatus : "VALID";
        if (!detectedMissing.isEmpty() && "VALID".equalsIgnoreCase(finalQuality)) {
            finalQuality = detectedMissing.size() > 3 ? "UNAVAILABLE" : "MISSING";
        }

        ForecastFeatureVector vector = new ForecastFeatureVector(
                cityId,
                h3Index,
                baseTimestamp,
                snapshotId,
                finalQuality,
                detectedMissing,
                verifiedMap,
                orderedValues,
                provenanceMap
        );

        // Validate vector structure, constraints, and quality integrity
        validator.validate(vector);

        return vector;
    }

    private boolean isLegitimatePhysicalZeroFeature(String featName) {
        return switch (featName) {
            case "fire_count_24h_25km", "fire_frp_sum_24h_25km", "fire_frp_mean_24h_25km",
                 "fire_frp_distance_decay", "fire_upwind_alignment_score", "rainfall",
                 "monitoring_coverage_gap_flag", "industrial_zone_within_2km_flag" -> true;
            default -> false;
        };
    }
}
