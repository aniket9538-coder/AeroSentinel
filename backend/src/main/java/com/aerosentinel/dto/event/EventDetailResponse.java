package com.aerosentinel.dto.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventDetailResponse(
        String eventId,
        UUID cityId,
        String h3CellId,
        Instant startedAt,
        Instant endedAt,
        Double riskScore,
        String riskLevel,
        Double confidence,
        String status,
        List<EventEvidenceDto> evidences,
        Instant createdAt,
        Instant updatedAt
) {}