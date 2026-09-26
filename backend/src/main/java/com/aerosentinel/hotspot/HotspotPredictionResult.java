package com.aerosentinel.hotspot;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable evaluation result produced by a HotspotDetectionEngine.
 * Contains the normalized risk score, risk level classification, confidence, and engine metadata.
 */
public record HotspotPredictionResult(
        double riskScore,
        HotspotRiskLevel riskLevel,
        double confidence,
        String modelVersion,
        String engineType,
        Map<String, Object> metadata
) {
    public static HotspotPredictionResult of(
            double riskScore,
            HotspotRiskLevel riskLevel,
            double confidence,
            String modelVersion,
            String engineType
    ) {
        return new HotspotPredictionResult(
                riskScore,
                riskLevel,
                confidence,
                modelVersion,
                engineType,
                Collections.emptyMap()
        );
    }
}
