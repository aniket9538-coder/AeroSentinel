package com.aerosentinel.dto.alert;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        String alertId,
        UUID eventId,
        String severity,
        String title,
        String description,
        String h3CellId,
        Double riskScore,
        Double confidence,
        Double expectedSpike,
        String evidenceSummary,
        String recommendedAction,
        String status,
        Instant createdAt,
        Instant acknowledgedAt,
        Instant closedAt
) {}