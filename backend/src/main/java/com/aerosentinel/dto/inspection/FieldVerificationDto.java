package com.aerosentinel.dto.inspection;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldVerificationDto(
        UUID id,
        UUID inspectionId,
        UUID alertId,
        UUID eventId,
        String h3Index,
        UUID predictionId,
        String verificationStatus,
        String verificationResult,
        String observedConditions,
        String inspectorNotes,
        String evidenceReferences,
        String verifiedBy,
        Instant inspectedAt,
        Instant createdAt
) {}
