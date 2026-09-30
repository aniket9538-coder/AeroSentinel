package com.aerosentinel.monitoring.dto;

import com.aerosentinel.monitoring.MonitoringPriority;

import java.time.Instant;
import java.util.UUID;

/**
 * Authoritative response contract for F8 monitoring priority calculation.
 * Encapsulates the inputs from F3 risk, F4 forecast uncertainty, F8-P2 observation coverage,
 * normalized intermediate features, final priority score, classification level,
 * and auditable configuration parameters.
 */
public record MonitoringPriorityResponse(
        String h3Index,
        Double latitude,
        Double longitude,

        // F3 Hotspot Risk Inputs
        Double riskScore,
        String riskLevel,
        Double f3Confidence,
        UUID predictionId,
        Instant predictionTimestamp,

        // F4 Forecast & Uncertainty Inputs
        Integer forecastHorizonHours,
        Double predictedPm25,
        Double lowerBound,
        Double upperBound,
        Double uncertaintyIntervalWidth,
        Double normalizedUncertainty,
        Instant forecastGeneratedAt,

        // F8-P2 Spatial Monitoring Coverage Inputs
        UUID nearestStationId,
        String nearestStationCode,
        String nearestStationName,
        Double nearestStationDistanceKm,
        Integer stationsWithin5kmCount,
        Integer monitoringCoverageGapFlag,

        // Computed Priority Outputs
        Double normalizedRisk,
        Double normalizedDistance,
        Double priorityScore,
        Integer priorityScorePercent,
        MonitoringPriority priorityLevel,

        // Configuration & Audit Parameters
        Double riskWeight,
        Double uncertaintyWeight,
        Double distanceWeight,
        Double uncertaintyMaxIntervalWidth,
        Double distanceMaxKm,
        Integer mediumThreshold,
        Integer highThreshold
) {}
