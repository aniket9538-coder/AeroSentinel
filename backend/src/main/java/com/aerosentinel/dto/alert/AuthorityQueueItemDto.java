package com.aerosentinel.dto.alert;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Authoritative Unified Authority Queue Item DTO for Feature 5 (F5-P5/P6) and Feature 7 (F7-P2).
 * Connects F3 Hotspot detection, F4 Forecast, F5 Evidence scoring, Gemini reasoning,
 * F5-P6 Field Team Assignment & Verification, and F7-P2 Citizen Evidence into an actionable, auditable queue record.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorityQueueItemDto(
        UUID alertId,
        UUID eventId,
        String eventCode,
        String h3Index,
        UUID parentPredictionId,
        UUID cityId,
        String cityName,
        String status,
        String severity,
        Double riskScore,
        Double evidenceScore,
        String triageState,
        String consistency,
        String title,
        String message,
        String forecastSummary,
        String recommendedAction,
        Boolean hasGeminiAnalysis,
        String geminiSummary,
        Instant createdAt,
        Instant updatedAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        // F5-P6 Operational Field Team Assignment & Verification Fields
        UUID activeAssignmentId,
        UUID assignedTeamId,
        String assignedTeamName,
        String assignmentStatus,
        Instant assignedAt,
        String verificationResult,
        String verificationStatus,
        Instant inspectedAt,
        // F7-P2 Attached Citizen Observations
        List<CitizenEvidenceItemDto> citizenEvidence
) {
    /**
     * Backward-compatible 31-parameter constructor for F5-P6 callers.
     */
    public AuthorityQueueItemDto(
            UUID alertId,
            UUID eventId,
            String eventCode,
            String h3Index,
            UUID parentPredictionId,
            UUID cityId,
            String cityName,
            String status,
            String severity,
            Double riskScore,
            Double evidenceScore,
            String triageState,
            String consistency,
            String title,
            String message,
            String forecastSummary,
            String recommendedAction,
            Boolean hasGeminiAnalysis,
            String geminiSummary,
            Instant createdAt,
            Instant updatedAt,
            Instant acknowledgedAt,
            Instant resolvedAt,
            UUID activeAssignmentId,
            UUID assignedTeamId,
            String assignedTeamName,
            String assignmentStatus,
            Instant assignedAt,
            String verificationResult,
            String verificationStatus,
            Instant inspectedAt
    ) {
        this(
                alertId, eventId, eventCode, h3Index, parentPredictionId,
                cityId, cityName, status, severity, riskScore, evidenceScore,
                triageState, consistency, title, message, forecastSummary,
                recommendedAction, hasGeminiAnalysis, geminiSummary,
                createdAt, updatedAt, acknowledgedAt, resolvedAt,
                activeAssignmentId, assignedTeamId, assignedTeamName,
                assignmentStatus, assignedAt, verificationResult,
                verificationStatus, inspectedAt, null
        );
    }

    /**
     * Backward-compatible 23-parameter constructor for F5-P5 callers.
     */
    public AuthorityQueueItemDto(
            UUID alertId,
            UUID eventId,
            String eventCode,
            String h3Index,
            UUID parentPredictionId,
            UUID cityId,
            String cityName,
            String status,
            String severity,
            Double riskScore,
            Double evidenceScore,
            String triageState,
            String consistency,
            String title,
            String message,
            String forecastSummary,
            String recommendedAction,
            Boolean hasGeminiAnalysis,
            String geminiSummary,
            Instant createdAt,
            Instant updatedAt,
            Instant acknowledgedAt,
            Instant resolvedAt
    ) {
        this(
                alertId, eventId, eventCode, h3Index, parentPredictionId,
                cityId, cityName, status, severity, riskScore, evidenceScore,
                triageState, consistency, title, message, forecastSummary,
                recommendedAction, hasGeminiAnalysis, geminiSummary,
                createdAt, updatedAt, acknowledgedAt, resolvedAt,
                null, null, null, null, null, null, null, null, null
        );
    }
}
