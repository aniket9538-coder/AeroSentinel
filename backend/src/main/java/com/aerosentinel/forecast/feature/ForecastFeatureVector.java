package com.aerosentinel.forecast.feature;

import java.time.Instant;
import java.util.*;

/**
 * Immutable domain representation of the exact 36 numeric features required
 * for F4 multi-horizon PM2.5 forecasting.
 *
 * Maintains deterministic ordering matching the authoritative artifact:
 * ai-service/models/artifacts/forecast_regressors_v1.joblib
 */
public record ForecastFeatureVector(
        UUID cityId,
        String h3Index,
        Instant baseTimestamp,
        UUID featureSnapshotId,
        String qualityStatus,
        List<String> missingFields,
        Map<String, Double> features,
        double[] orderedValues,
        Map<String, FeatureProvenance> featureProvenance
) {

    public static final int F4_FEATURE_COUNT = 36;
    public static final String SCHEMA_VERSION = "f4-features-v1";

    public static final List<String> ORDERED_FEATURE_NAMES = List.of(
            "latitude",
            "longitude",
            "pm10",
            "no2",
            "so2",
            "co",
            "o3",
            "hour",
            "day_of_week",
            "is_weekend",
            "hour_sin",
            "hour_cos",
            "dow_sin",
            "dow_cos",
            "temperature",
            "humidity",
            "wind_speed",
            "wind_direction",
            "wind_u",
            "wind_v",
            "rainfall",
            "pressure",
            "pm25_spatial_lag_mean",
            "nearest_station_distance_km",
            "stations_within_5km_count",
            "monitoring_coverage_gap_flag",
            "dist_to_nearest_industrial_km",
            "dist_to_nearest_major_road_km",
            "sensitive_receptors_count_2km",
            "industrial_zone_within_2km_flag",
            "fire_count_24h_25km",
            "fire_frp_sum_24h_25km",
            "fire_frp_mean_24h_25km",
            "nearest_fire_distance_km",
            "fire_frp_distance_decay",
            "fire_upwind_alignment_score"
    );

    public ForecastFeatureVector {
        Objects.requireNonNull(h3Index, "h3Index must not be null");
        Objects.requireNonNull(baseTimestamp, "baseTimestamp must not be null");
        Objects.requireNonNull(features, "features map must not be null");
        Objects.requireNonNull(orderedValues, "orderedValues array must not be null");

        if (orderedValues.length != F4_FEATURE_COUNT) {
            throw new IllegalArgumentException("Expected exactly " + F4_FEATURE_COUNT + " values, got " + orderedValues.length);
        }

        missingFields = missingFields != null ? List.copyOf(missingFields) : List.of();
        features = Collections.unmodifiableMap(new LinkedHashMap<>(features));
        orderedValues = Arrays.copyOf(orderedValues, orderedValues.length);
        featureProvenance = featureProvenance != null ? Collections.unmodifiableMap(new LinkedHashMap<>(featureProvenance)) : Collections.emptyMap();
    }

    public ForecastFeatureVector(
            UUID cityId,
            String h3Index,
            Instant baseTimestamp,
            UUID featureSnapshotId,
            String qualityStatus,
            List<String> missingFields,
            Map<String, Double> features,
            double[] orderedValues
    ) {
        this(cityId, h3Index, baseTimestamp, featureSnapshotId, qualityStatus, missingFields, features, orderedValues, Collections.emptyMap());
    }

    public double[] getOrderedValuesCopy() {
        return Arrays.copyOf(orderedValues, orderedValues.length);
    }

    public Double getFeature(String name) {
        return features.get(name);
    }

    public FeatureProvenance getProvenance(String name) {
        return featureProvenance.getOrDefault(name, FeatureProvenance.VALID_OBSERVATION);
    }

    public boolean isFullyValid() {
        return "VALID".equalsIgnoreCase(qualityStatus) && missingFields.isEmpty();
    }
}
