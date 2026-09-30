package com.aerosentinel.dto.authority;

import java.time.Instant;
import java.util.UUID;

public record AuthorityQueueItemResponse(
        String alertId,
        String eventId,
        UUID cityId,
        String h3CellId,
        String severity,
        String title,
        Double riskScore,
        Double confidence,
        String eventStatus,
        String alertStatus,
        Instant createdAt
) {}