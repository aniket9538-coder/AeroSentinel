package com.aerosentinel.dto.action;

import java.time.Instant;
import java.util.UUID;

public record ActionResponseDto(
        UUID id,
        UUID alertId,
        UUID eventId,
        String actionType,
        String actionDetails,
        String performedBy,
        Instant performedAt,
        String eventStatus,
        String alertStatus
) {}
