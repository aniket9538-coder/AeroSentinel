package com.aerosentinel.feature;

/**
 * Explicit feature quality status distinguishing complete observations, missing predictors,
 * suspicious bounds, and uninstrumented telemetry streams.
 */
public enum FeatureQualityStatus {
    VALID,
    MISSING,
    SUSPECT,
    UNAVAILABLE
}
