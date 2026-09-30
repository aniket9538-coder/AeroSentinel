package com.aerosentinel.dto.authority;

import java.time.Instant;
import java.util.UUID;

public record InspectionResponse(
        String inspectionId,
        UUID eventId,
        String teamId,
        String status,
        String fieldNotes,
        String verificationStatus,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {}