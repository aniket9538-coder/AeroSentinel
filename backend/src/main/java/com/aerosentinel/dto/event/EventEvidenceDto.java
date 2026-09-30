package com.aerosentinel.dto.event;

import java.time.Instant;
import java.util.UUID;

public record EventEvidenceDto(
        UUID id,
        String evidenceType,
        String sourceId,
        String valueSummary,
        Double strength,
        Instant observedAt,
        Instant createdAt
) {}