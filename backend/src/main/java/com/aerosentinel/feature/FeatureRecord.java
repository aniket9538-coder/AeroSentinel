package com.aerosentinel.feature;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable canonical feature record matching the exact 36-feature contract of the
 * calibrated Random Forest model (hotspot_classifier_v1.joblib) and multi-horizon forecasters.
 */
public record FeatureRecord(
        UUID cityId,
        String h3Index,
        Instant observedAt,
        String featureSchemaVersion,
        Map<String, Object> features,
        FeatureQualityStatus qualityStatus,
        List<String> missingFeatures
) {
    public static final String SCHEMA_VERSION = "f3-features-v1";
    public static final int FEATURE_COUNT = 36;

    /**
     * Authoritative, ordered feature contract extracted directly from trained artifact
     * hotspot_classifier_v1.joblib and train_all_models.py select_features().
     */
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
}
