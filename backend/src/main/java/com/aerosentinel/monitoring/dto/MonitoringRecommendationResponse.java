package com.aerosentinel.monitoring.dto;

import com.aerosentinel.monitoring.MonitoringPriority;
import com.aerosentinel.monitoring.MonitoringRecommendationType;

import java.time.Instant;
import java.util.UUID;

/**
 * Authoritative response contract for F8 monitoring recommendations.
 * Inherits all audited lineage and priority factors from F8-P3,
 * preserves frontend compatibility fields (uncertainty, stationDistanceKm, priorityScore),
 * and adds actionable decision-support recommendation guidance and deterministic rationale.
 */
public record MonitoringRecommendationResponse(
        // Identity & Spatial
        String h3Index,
        Double latitude,
        Double longitude,

        // F3 Hotspot Risk
        Double riskScore,
        String riskLevel,
        Double f3Confidence,
        UUID predictionId,
        Instant predictionTimestamp,

        // F4 Forecast & Uncertainty
        Integer forecastHorizonHours,
        Double predictedPm25,
        Double lowerBound,
        Double upperBound,
        Double uncertaintyIntervalWidth,
        Double normalizedUncertainty,
        Instant forecastGeneratedAt,

        // F8-P2 Spatial Monitoring Coverage
        UUID nearestStationId,
        String nearestStationCode,
        String nearestStationName,
        Double nearestStationDistanceKm,
        Integer stationsWithin5kmCount,
        Integer monitoringCoverageGapFlag,

        // Priority Scoring (F8-P3)
        Double normalizedRisk,
        Double normalizedDistance,
        Double priorityScore,
        Integer priorityScorePercent,
        MonitoringPriority priorityLevel,

        // F8-P4 Actionable Recommendation
        MonitoringRecommendationType recommendationType,
        String recommendation,
        String rationale,

        // Frontend Backward Compatibility Aliases
        Double uncertainty,
        Double stationDistanceKm
) {
    /**
     * Factory constructor translating an F8-P3 priority evaluation and recommendation outputs
     * into the authoritative recommendation contract.
     */
    public static MonitoringRecommendationResponse from(
            MonitoringPriorityResponse p3,
            MonitoringRecommendationType recommendationType,
            String recommendation,
            String rationale
    ) {
        if (p3 == null) {
            throw new IllegalArgumentException("MonitoringPriorityResponse must not be null");
        }

        // Priority score normalized to 0-100 scale (e.g. 13.09) for frontend display
        double scoreOutOf100 = p3.priorityScore() != null
                ? Math.round(p3.priorityScore() * 10000.0) / 100.0
                : 0.0;

        return new MonitoringRecommendationResponse(
                p3.h3Index(),
                p3.latitude(),
                p3.longitude(),
                p3.riskScore(),
                p3.riskLevel(),
                p3.f3Confidence(),
                p3.predictionId(),
                p3.predictionTimestamp(),
                p3.forecastHorizonHours(),
                p3.predictedPm25(),
                p3.lowerBound(),
                p3.upperBound(),
                p3.uncertaintyIntervalWidth(),
                p3.normalizedUncertainty(),
                p3.forecastGeneratedAt(),
                p3.nearestStationId(),
                p3.nearestStationCode(),
                p3.nearestStationName(),
                p3.nearestStationDistanceKm(),
                p3.stationsWithin5kmCount(),
                p3.monitoringCoverageGapFlag(),
                p3.normalizedRisk(),
                p3.normalizedDistance(),
                scoreOutOf100,
                p3.priorityScorePercent(),
                p3.priorityLevel(),
                recommendationType,
                recommendation,
                rationale,
                // Frontend compatibility mappings:
                p3.normalizedUncertainty(),
                p3.nearestStationDistanceKm()
        );
    }
}
