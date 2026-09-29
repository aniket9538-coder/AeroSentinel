package com.aerosentinel.dto.event;

import com.aerosentinel.dto.alert.CitizenEvidenceItemDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Authoritative Unified Context Contract for a Potential Pollution Event (Feature 7).
 *
 * Implements strict separation:
 * 1. Prediction context (F3 Random Forest risk score & level)
 * 2. Forecast context (F4 Multi-horizon predictions: +1h, +3h, +6h)
 * 3. Evidence context (F5 Multi-source scoring, consistency & triage)
 * 4. Citizen Evidence (F6 Ground observations & Gemini Vision auxiliary interpretation)
 * 5. Alert Context (F7 Authority queue candidate, if authoritative threshold was met)
 *
 * Prediction != Event != Alert != Action
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PollutionEventContextDto(
        // Canonical Event identifiers & lifecycle attributes (for 100% backward compatibility)
        UUID id,
        String eventCode,
        UUID gridCellId,
        String h3Index,
        UUID cityId,
        String cityName,
        UUID predictionId,
        String severity,
        String status,
        Instant startedAt,
        Instant resolvedAt,
        Instant createdAt,

        // Nested Event summary
        EventSummaryDto event,

        // F3 Hotspot Prediction Context
        PredictionContextDto prediction,

        // F4 Multi-Horizon Forecast Context
        ForecastContextDto forecast,

        // F5 Evidence & Triage Context
        EvidenceContextDto evidence,

        // F6 Citizen Auxiliary Evidence
        List<CitizenEvidenceItemDto> citizenEvidence,

        // F7 Alert Context (if alert candidate was generated)
        AlertContextDto alert
) {
    public record EventSummaryDto(
            UUID id,
            String eventCode,
            UUID gridCellId,
            String h3Index,
            UUID predictionId,
            String severity,
            String status,
            Instant startedAt,
            Instant resolvedAt,
            Instant createdAt
    ) {}

    public record PredictionContextDto(
            UUID predictionId,
            String h3Index,
            Double riskScore,
            String riskLevel,
            Double confidence,
            Double operationalThreshold,
            String modelVersion,
            Instant predictedAt
    ) {}

    public record ForecastContextDto(
            boolean available,
            String status, // "AVAILABLE" or "UNAVAILABLE"
            UUID parentPredictionId,
            String forecastModelVersion,
            Instant baseTimestamp,
            Instant generatedAt,
            Double forecastConfidence,
            List<HorizonDto> horizons
    ) {
        public record HorizonDto(
                int horizonHours,
                Instant targetTime,
                double predictedPm25,
                double lowerBound,
                double upperBound,
                String unit
        ) {}
    }

    public record EvidenceContextDto(
            Double evidenceScore,
            String consistency,
            String triageState,
            Double completeness,
            int signalsCount,
            List<SignalSummaryDto> signals
    ) {
        public record SignalSummaryDto(
                String signalId,
                String sourceType,
                String dataSource,
                String relevanceTier,
                String description,
                Double confidenceScore,
                Instant observedAt
        ) {}
    }

    public record AlertContextDto(
            boolean alertExists,
            UUID alertId,
            String status,
            String severity,
            String triageState,
            String title,
            String message,
            Instant acknowledgedAt,
            Instant resolvedAt,
            String assignedTeamName,
            String verificationResult
    ) {}
}
