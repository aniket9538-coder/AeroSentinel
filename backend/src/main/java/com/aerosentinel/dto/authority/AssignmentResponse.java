package com.aerosentinel.dto.authority;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        String eventId,
        String teamId,
        String teamName,
        String eventStatus,
        UUID assignedBy,
        Instant assignedAt
) {}