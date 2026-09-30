package com.aerosentinel.dto.event;

import java.time.Instant;
import java.util.UUID;

public record PollutionEventResponse(
        String eventId,
        UUID cityId,
        String h3CellId,
        Instant startedAt,
        Instant endedAt,
        Double riskScore,
        String riskLevel,
        Double confidence,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public String h3Index() {
        return h3CellId;
    }
}