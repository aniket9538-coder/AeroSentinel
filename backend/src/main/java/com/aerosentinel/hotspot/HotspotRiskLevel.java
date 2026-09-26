package com.aerosentinel.hotspot;

/**
 * Domain enumeration for Hotspot Risk Levels.
 * Categorizes risk scores into standard environmental alert tiers.
 */
public enum HotspotRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;

    /**
     * Determines risk level from a normalized risk score [0.0, 1.0] (Baseline Engine).
     *
     * @param score normalized risk score
     * @return corresponding HotspotRiskLevel
     */
    public static HotspotRiskLevel fromScore(double score) {
        if (score >= 0.85) {
            return CRITICAL;
        } else if (score >= 0.65) {
            return HIGH;
        } else if (score >= 0.40) {
            return MODERATE;
        } else {
            return LOW;
        }
    }

    /**
     * Determines risk level from calibrated ML hotspot probability and operational threshold (ML Engine).
     *
     * Operational Decision Rule:
     * - p < threshold (0.20): LOW (below operational emergence threshold)
     * - threshold <= p < 0.40: MODERATE (potential hotspot detected, early advisory)
     * - 0.40 <= p < 0.70: HIGH (elevated hotspot likelihood)
     * - p >= 0.70: CRITICAL (acute hotspot emergence)
     *
     * @param probability calibrated model probability [0.0, 1.0]
     * @param operationalThreshold operational cutoff (typically 0.20 from artifact)
     * @return corresponding HotspotRiskLevel
     */
    public static HotspotRiskLevel fromProbability(double probability, double operationalThreshold) {
        if (probability >= 0.70) {
            return CRITICAL;
        } else if (probability >= 0.40) {
            return HIGH;
        } else if (probability >= operationalThreshold) {
            return MODERATE;
        } else {
            return LOW;
        }
    }
}

