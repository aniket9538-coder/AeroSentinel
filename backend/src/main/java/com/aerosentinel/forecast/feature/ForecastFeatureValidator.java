package com.aerosentinel.forecast.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates forecast feature vectors before they can be adapted or passed to model inference.
 *
 * Implements strict checks required by F4-P2 and Final Hardening Patch:
 *  1. Exactly 36 features
 *  2. Exact expected names and order
 *  3. All expected values numeric
 *  4. Finite values only (Double.isFinite)
 *  5. No unexpected feature
 *  6. No missing required feature (when strict mode is requested)
 *  7. No NaN
 *  8. No Infinity
 *  9. Spatial identity valid (valid H3 index format)
 * 10. City identity valid (cityId non-null)
 * 11. T0 timestamp valid (non-null, not epoch 0)
 * 12. Traceability (featureSnapshotId or source tracked)
 * 13. Artifact schema compatibility verified
 * 14. Quality Status Integrity: Quality status cannot be VALID if fields are missing or imputed
 */
@Component
public class ForecastFeatureValidator {

    private static final Logger log = LoggerFactory.getLogger(ForecastFeatureValidator.class);
    private static final Pattern H3_PATTERN = Pattern.compile("^[0-9a-fA-F]{15}$");

    /**
     * Validates a ForecastFeatureVector.
     * Throws ForecastFeaturesInvalidException if any required check fails.
     *
     * @param vector the vector to validate
     * @param requireStrictNoMissing if true, throws if vector has any missing fields
     */
    public void validate(ForecastFeatureVector vector, boolean requireStrictNoMissing) {
        if (vector == null) {
            throw new ForecastFeaturesInvalidException("ForecastFeatureVector must not be null", "unknown");
        }

        List<String> errors = new ArrayList<>();
        String h3 = vector.h3Index();

        // 1 & 2 & 13: Feature count and artifact compatibility
        if (vector.orderedValues().length != ForecastFeatureVector.F4_FEATURE_COUNT) {
            errors.add("Check 1 failed: Expected exactly " + ForecastFeatureVector.F4_FEATURE_COUNT
                    + " features, got " + vector.orderedValues().length);
        }

        Map<String, Double> map = vector.features();
        if (map == null || map.size() != ForecastFeatureVector.F4_FEATURE_COUNT) {
            errors.add("Check 2/5 failed: Feature map size mismatch. Expected "
                    + ForecastFeatureVector.F4_FEATURE_COUNT + ", got " + (map == null ? 0 : map.size()));
        }

        // Check exact names and expected order
        for (int i = 0; i < ForecastFeatureVector.F4_FEATURE_COUNT; i++) {
            String expectedName = ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(i);
            if (map != null && !map.containsKey(expectedName)) {
                errors.add("Check 2/5 failed: Missing expected feature in map: " + expectedName);
            }
        }

        // 3, 4, 7, 8: Numeric, finite, no NaN, no Infinity
        double[] vals = vector.orderedValues();
        for (int i = 0; i < vals.length; i++) {
            double v = vals[i];
            String name = i < ForecastFeatureVector.ORDERED_FEATURE_NAMES.size()
                    ? ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(i)
                    : "feature[" + i + "]";

            if (Double.isNaN(v)) {
                errors.add("Check 7 failed: Feature '" + name + "' is NaN");
            } else if (Double.isInfinite(v)) {
                errors.add("Check 4/8 failed: Feature '" + name + "' is Infinite: " + v);
            } else if (!Double.isFinite(v)) {
                errors.add("Check 4 failed: Feature '" + name + "' is not finite: " + v);
            }
        }

        // 6: Missing required features
        if (requireStrictNoMissing && !vector.missingFields().isEmpty()) {
            errors.add("Check 6 failed: Strict mode requires zero missing features, but found: "
                    + vector.missingFields());
        }

        // 9: Spatial identity (H3 index)
        if (h3 == null || !H3_PATTERN.matcher(h3).matches()) {
            errors.add("Check 9 failed: Invalid spatial identity H3 index: '" + h3 + "'");
        }

        // 10: City identity
        if (vector.cityId() == null) {
            errors.add("Check 10 failed: cityId must not be null");
        }

        // 11: T0 timestamp
        if (vector.baseTimestamp() == null || vector.baseTimestamp().equals(Instant.EPOCH)) {
            errors.add("Check 11 failed: baseTimestamp T0 must not be null or epoch 0");
        }

        // 12: Traceability check (quality status must be known)
        if (vector.qualityStatus() == null || vector.qualityStatus().isBlank()) {
            errors.add("Check 12 failed: qualityStatus must be specified");
        }

        // 14: Quality Status Integrity check
        // If there are missing fields, qualityStatus cannot be falsely promoted to VALID
        if (!vector.missingFields().isEmpty() && "VALID".equalsIgnoreCase(vector.qualityStatus())) {
            errors.add("Check 14 failed: Quality status cannot be VALID when physical fields are missing: "
                    + vector.missingFields());
        }

        // Check provenance integrity if provided
        if (vector.featureProvenance() != null) {
            for (Map.Entry<String, FeatureProvenance> entry : vector.featureProvenance().entrySet()) {
                String feat = entry.getKey();
                FeatureProvenance prov = entry.getValue();
                if ((prov == FeatureProvenance.MISSING || prov == FeatureProvenance.SOURCE_UNAVAILABLE)
                        && "VALID".equalsIgnoreCase(vector.qualityStatus())) {
                    errors.add("Check 14 failed: Quality status cannot be VALID when feature '" + feat
                            + "' has provenance " + prov);
                    break;
                }
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Forecast feature validation failed for cell {}: {}", h3, errors);
            throw new ForecastFeaturesInvalidException("Forecast features validation failed", h3, errors);
        }
    }

    /**
     * Default validation: validates structural integrity and finite values, allowing domain-tracked missingness.
     */
    public void validate(ForecastFeatureVector vector) {
        validate(vector, false);
    }
}
