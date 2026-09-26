package com.aerosentinel.hotspot;

import com.aerosentinel.feature.FeatureSnapshot;

/**
 * Provider-agnostic interface for Hotspot Detection Engines.
 * Implementations evaluate an immutable FeatureSnapshot and produce a validated HotspotPredictionResult.
 * Allows seamless switching between Baseline and ML-backed inference engines.
 */
public interface HotspotDetectionEngine {

    /**
     * Engine or model version identifier (e.g., "hotspot-baseline-v1", "hotspot_classifier_v1").
     */
    String getEngineVersion();

    /**
     * Type identifier of the detection engine ("BASELINE", "ML", "ENSEMBLE").
     */
    String getEngineType();

    /**
     * Evaluates the given FeatureSnapshot to produce a risk assessment result.
     *
     * @param snapshot the immutable feature snapshot
     * @return HotspotPredictionResult containing riskScore, riskLevel, confidence, and engine metadata
     */
    HotspotPredictionResult evaluate(FeatureSnapshot snapshot);
}
