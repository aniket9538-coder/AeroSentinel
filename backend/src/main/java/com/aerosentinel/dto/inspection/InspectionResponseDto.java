package com.aerosentinel.dto.inspection;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InspectionResponseDto(
        UUID id,
        UUID alertId,
        UUID teamId,
        String teamCode,
        String teamName,
        UUID eventId,
        String h3Index,
        UUID predictionId,
        UUID assignedBy,
        Instant assignedAt,
        Instant scheduledAt,
        Instant startedAt,
        Instant completedAt,
        String findings,
        String notes,
        String status,
        Instant createdAt,
        Instant updatedAt,
        FieldVerificationDto latestVerification
) {}
