package com.aerosentinel.hotspot;

import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * Adapter boundary between persistent FeatureSnapshots and future ML models (e.g. hotspot_classifier_v1.joblib).
 *
 * Responsibilities:
 * 1. Schema version validation (must match 'f3-features-v1').
 * 2. Exact 36-feature name and order validation matching ORDERED_FEATURE_NAMES.
 * 3. Strict numeric type conversion and non-null validation.
 * 4. Deterministic unit normalization boundary (wind_speed km/h -> m/s without double conversion).
 * 5. Explicit missingness tracking.
 */
@Component
public class ModelFeatureVectorAdapter {

    private static final Logger log = LoggerFactory.getLogger(ModelFeatureVectorAdapter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String EXPECTED_SCHEMA_VERSION = FeatureRecord.SCHEMA_VERSION;
    public static final int EXPECTED_FEATURE_COUNT = 36;

    public record AdaptedFeatureVector(
            String h3Index,
            String featureSchemaVersion,
            double[] orderedValues,
            Map<String, Double> featureMap,
            List<String> missingFeatures,
            boolean isFullyValid
    ) {}

    /**
     * Adapts a FeatureSnapshot into an ordered, verified 36-feature vector ready for model evaluation.
     *
     * @param snapshot the persistent FeatureSnapshot
     * @return AdaptedFeatureVector containing ordered values in exact model contract order
     */
    public AdaptedFeatureVector adapt(FeatureSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("FeatureSnapshot must not be null");
        }

        if (!EXPECTED_SCHEMA_VERSION.equals(snapshot.getFeatureSchemaVersion())) {
            throw new IllegalStateException("Feature schema version mismatch: expected "
                    + EXPECTED_SCHEMA_VERSION + " but got " + snapshot.getFeatureSchemaVersion());
        }

        Map<String, Object> rawMap;
        try {
            rawMap = MAPPER.readValue(snapshot.getFeatures(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse snapshot features JSON for H3 cell " + snapshot.getH3Index(), e);
        }

        double[] orderedValues = new double[EXPECTED_FEATURE_COUNT];
        Map<String, Double> verifiedMap = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        if (snapshot.getMissingFeatures() != null) {
            missing.addAll(Arrays.asList(snapshot.getMissingFeatures()));
        }

        for (int i = 0; i < EXPECTED_FEATURE_COUNT; i++) {
            String featName = FeatureRecord.ORDERED_FEATURE_NAMES.get(i);
            Object rawVal = rawMap.get(featName);

            if (rawVal == null) {
                if (!missing.contains(featName)) {
                    missing.add(featName);
                }
                orderedValues[i] = 0.0;
                verifiedMap.put(featName, 0.0);
            } else if (rawVal instanceof Number num) {
                double val = num.doubleValue();
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    throw new IllegalStateException("Non-finite numeric value encountered for feature " + featName + ": " + val);
                }
                orderedValues[i] = val;
                verifiedMap.put(featName, val);
            } else {
                throw new IllegalStateException("Non-numeric value encountered for feature " + featName + ": " + rawVal);
            }
        }

        boolean fullyValid = missing.isEmpty() && "VALID".equalsIgnoreCase(snapshot.getQualityStatus());

        return new AdaptedFeatureVector(
                snapshot.getH3Index(),
                snapshot.getFeatureSchemaVersion(),
                orderedValues,
                verifiedMap,
                missing,
                fullyValid
        );
    }

    /**
     * Extracts model-facing vector with normalized wind speed (m/s).
     * Applied strictly at this adapter boundary to guarantee no double conversion.
     *
     * @param snapshot the persistent snapshot
     * @param normalizeWindSpeedToMps true if model strictly requires wind_speed in m/s
     * @return AdaptedFeatureVector with wind_speed normalized
     */
    public AdaptedFeatureVector adaptWithWindNormalization(FeatureSnapshot snapshot, boolean normalizeWindSpeedToMps) {
        AdaptedFeatureVector base = adapt(snapshot);
        if (!normalizeWindSpeedToMps) {
            return base;
        }

        double[] normalized = Arrays.copyOf(base.orderedValues(), base.orderedValues().length);
        Map<String, Double> normMap = new LinkedHashMap<>(base.featureMap());

        int windSpeedIndex = FeatureRecord.ORDERED_FEATURE_NAMES.indexOf("wind_speed");
        if (windSpeedIndex >= 0) {
            double currentSpeedKmh = normalized[windSpeedIndex];
            double speedMps = currentSpeedKmh / 3.6;
            normalized[windSpeedIndex] = Math.round(speedMps * 100.0) / 100.0;
            normMap.put("wind_speed", normalized[windSpeedIndex]);
        }

        return new AdaptedFeatureVector(
                base.h3Index(),
                base.featureSchemaVersion(),
                normalized,
                normMap,
                base.missingFeatures(),
                base.isFullyValid()
        );
    }
}
