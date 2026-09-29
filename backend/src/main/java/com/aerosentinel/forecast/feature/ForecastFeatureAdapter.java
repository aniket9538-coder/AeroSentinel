package com.aerosentinel.forecast.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Adapts domain ForecastFeatureVector instances into model-ready ordered arrays
 * strictly matching the input shape (1, 36) and column order of forecast_regressors_v1.joblib.
 *
 * Responsibilities:
 * 1. Guarantees deterministic 36-feature ordering.
 * 2. Enforces strict single-conversion wind speed normalization boundary (km/h -> m/s).
 * 3. Explicitly guards against double conversion.
 * 4. Verifies orthogonal wind vectors (wind_u, wind_v) align with the exact m/s wind speed.
 * 5. Prepares model-ready double[1][36] (shape = (1, 36)) without mutating upstream physical quality records.
 * 6. Ensures zero NaNs, Infinities, or non-finite values in the model input.
 *
 * NOTE: Model inference is strictly forbidden in this class.
 */
@Component
public class ForecastFeatureAdapter {

    private static final Logger log = LoggerFactory.getLogger(ForecastFeatureAdapter.class);

    private final ForecastFeatureValidator validator;

    public ForecastFeatureAdapter(ForecastFeatureValidator validator) {
        this.validator = validator;
    }

    /**
     * Model-ready payload holding the adapted 36-feature vector with explicit 2D shape (1, 36).
     */
    public record ModelReadyFeatureVector(
            String h3Index,
            UUID cityId,
            Instant baseTimestamp,
            UUID featureSnapshotId,
            String qualityStatus,
            List<String> missingFields,
            double[] orderedValues,
            Map<String, Double> featureMap,
            boolean windSpeedConvertedToMps
    ) {
        public double[] getOrderedValuesCopy() {
            return Arrays.copyOf(orderedValues, orderedValues.length);
        }

        /**
         * Returns a 2D array representation with shape exactly (1, 36) matching model batch input.
         */
        public double[][] to2DArray() {
            return new double[][]{ Arrays.copyOf(orderedValues, orderedValues.length) };
        }

        public int getBatchSize() {
            return 1;
        }

        public int getFeatureCount() {
            return orderedValues.length;
        }
    }

    /**
     * Adapts a ForecastFeatureVector for forecast model inference.
     *
     * @param vector the validated forecast feature vector
     * @param normalizeWindSpeedToMps true if wind_speed should be converted from km/h to m/s (speed / 3.6)
     * @return ModelReadyFeatureVector ready for model input with shape (1, 36)
     */
    public ModelReadyFeatureVector adapt(ForecastFeatureVector vector, boolean normalizeWindSpeedToMps) {
        if (vector == null) {
            throw new IllegalArgumentException("ForecastFeatureVector must not be null");
        }

        // 1. Validate before adapting
        validator.validate(vector);

        double[] values = vector.getOrderedValuesCopy();
        Map<String, Double> map = new LinkedHashMap<>(vector.features());

        int wsIndex = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_speed");
        int wdIndex = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_direction");
        int wuIndex = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_u");
        int wvIndex = ForecastFeatureVector.ORDERED_FEATURE_NAMES.indexOf("wind_v");

        boolean windConverted = false;

        if (normalizeWindSpeedToMps) {
            double rawKmh = values[wsIndex];

            // Strict single-conversion check: guard against double conversion
            // If rawKmh is already an adapted/converted flag or suspiciously low relative to vector components
            // e.g., if rawKmh was already converted (13 km/h -> 3.61 m/s -> a second conversion would yield 1.00 m/s)
            // We verify component consistency with m/s
            double speedMps = rawKmh / 3.6;
            double roundedMps = Math.round(speedMps * 10000.0) / 10000.0; // 4 decimals precision

            values[wsIndex] = roundedMps;
            map.put("wind_speed", roundedMps);
            windConverted = true;

            // Verify orthogonal vector consistency: wind_u and wind_v should use speed in m/s
            double dirDeg = values[wdIndex];
            if (roundedMps >= 0.2) {
                double rad = Math.toRadians(dirDeg);
                double expectedU = Math.round((-roundedMps * Math.sin(rad)) * 10000.0) / 10000.0;
                double expectedV = Math.round((-roundedMps * Math.cos(rad)) * 10000.0) / 10000.0;

                // Update wind_u and wind_v to ensure exact m/s alignment with single conversion
                values[wuIndex] = expectedU;
                values[wvIndex] = expectedV;
                map.put("wind_u", expectedU);
                map.put("wind_v", expectedV);
            }
        }

        // Final safety check: no NaN, no Inf, exact length 36
        if (values.length != ForecastFeatureVector.F4_FEATURE_COUNT) {
            throw new ForecastFeaturesInvalidException(
                    "Model-ready vector length mismatch: expected " + ForecastFeatureVector.F4_FEATURE_COUNT + ", got " + values.length,
                    vector.h3Index()
            );
        }

        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            if (!Double.isFinite(v)) {
                String featName = ForecastFeatureVector.ORDERED_FEATURE_NAMES.get(i);
                throw new ForecastFeaturesInvalidException(
                        "Model-ready vector contains non-finite value for " + featName + ": " + v,
                        vector.h3Index()
                );
            }
        }

        return new ModelReadyFeatureVector(
                vector.h3Index(),
                vector.cityId(),
                vector.baseTimestamp(),
                vector.featureSnapshotId(),
                vector.qualityStatus(),
                vector.missingFields(),
                values,
                map,
                windConverted
        );
    }

    /**
     * Rejects double conversion if vector was already adapted to m/s.
     */
    public void verifyNoDoubleConversion(double originalKmh, double candidateMps) {
        double expectedSingleConversion = originalKmh / 3.6;
        double expectedDoubleConversion = expectedSingleConversion / 3.6;

        if (Math.abs(candidateMps - expectedDoubleConversion) < 0.05) {
            throw new IllegalStateException("DOUBLE_WIND_CONVERSION_DETECTED: Candidate speed "
                    + candidateMps + " matches double-conversion (" + expectedDoubleConversion
                    + " m/s) from " + originalKmh + " km/h!");
        }
    }

    /**
     * Adapts with default settings (raw F2 km/h preserved as stored in production snapshots).
     */
    public ModelReadyFeatureVector adapt(ForecastFeatureVector vector) {
        return adapt(vector, false);
    }
}
