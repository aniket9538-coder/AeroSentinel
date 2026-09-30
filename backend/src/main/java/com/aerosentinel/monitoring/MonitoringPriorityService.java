package com.aerosentinel.monitoring;

import org.springframework.stereotype.Service;

@Service
public class MonitoringPriorityService {

    private final MonitoringProperties properties;

    public MonitoringPriorityService(MonitoringProperties properties) {
        this.properties = properties;
    }

    public record PriorityEvaluation(
            MonitoringPriority priority,
            RecommendationType recommendationType,
            String recommendationReason
    ) {}

    /**
     * Evaluates monitoring priority deterministically based on risk, uncertainty, and nearest station distance.
     * Throws IllegalArgumentException on invalid ranges (Section 40 & 44).
     */
    public PriorityEvaluation evaluate(double riskScore, double uncertainty, double nearestStationDistanceKm) {
        if (riskScore < 0.0 || riskScore > 100.0) {
            throw new IllegalArgumentException("Risk score must be between 0.0 and 100.0. Provided: " + riskScore);
        }
        if (uncertainty < 0.0 || uncertainty > 1.0) {
            throw new IllegalArgumentException("Uncertainty must be between 0.0 and 1.0. Provided: " + uncertainty);
        }
        if (nearestStationDistanceKm < 0.0) {
            throw new IllegalArgumentException("Distance cannot be negative. Provided: " + nearestStationDistanceKm);
        }

        boolean isHighRisk = riskScore >= properties.getHighRiskThreshold();
        boolean isMediumRisk = riskScore >= properties.getMediumRiskThreshold();
        boolean isHighUncertainty = uncertainty >= properties.getHighUncertaintyThreshold();
        boolean isFarStation = nearestStationDistanceKm >= properties.getFarStationThresholdKm();
        boolean isPeripheralStation = nearestStationDistanceKm >= properties.getPeripheralStationThresholdKm();

        // Rule 1: High risk + high uncertainty + far station -> HIGH Priority & MOBILE_SENSOR
        if (isHighRisk && isHighUncertainty && isFarStation) {
            return new PriorityEvaluation(
                    MonitoringPriority.HIGH,
                    RecommendationType.MOBILE_SENSOR,
                    "High model risk combined with elevated uncertainty and weak fixed-station coverage. Deploy mobile monitoring unit."
            );
        }

        // Rule 2: High risk + (high uncertainty OR far station) -> HIGH Priority & FIELD_VERIFICATION
        if (isHighRisk && (isHighUncertainty || isFarStation)) {
            return new PriorityEvaluation(
                    MonitoringPriority.HIGH,
                    RecommendationType.FIELD_VERIFICATION,
                    "High model risk with observational uncertainty or sparse station coverage. On-site field verification recommended."
            );
        }

        // Rule 3: Moderate risk OR peripheral station distance -> MEDIUM Priority & MONITORING_REVIEW
        if (isMediumRisk || isPeripheralStation) {
            return new PriorityEvaluation(
                    MonitoringPriority.MEDIUM,
                    RecommendationType.MONITORING_REVIEW,
                    "Moderate risk or peripheral fixed-station distance. Periodic surveillance review recommended."
            );
        }

        // Rule 4: Low risk within station perimeter -> LOW Priority & NONE
        return new PriorityEvaluation(
                MonitoringPriority.LOW,
                RecommendationType.NONE,
                "Low model risk within acceptable fixed monitoring station perimeter."
        );
    }
}
