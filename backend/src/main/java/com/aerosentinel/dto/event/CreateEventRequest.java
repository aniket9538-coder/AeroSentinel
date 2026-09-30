package com.aerosentinel.dto.event;

import java.util.UUID;

public record CreateEventRequest(
        UUID cityId,
        String h3CellId,
        String source,
        Double riskScore,
        String notes,
        Double confidence
) {
    // 2-argument constructor used by Lifecycle tests
    public CreateEventRequest(String h3CellId, String notes) {
        this(null, h3CellId, "MANUAL", 50.0, notes, 0.80);
    }

    // 1-argument constructor
    public CreateEventRequest(String h3CellId) {
        this(null, h3CellId, "MANUAL", 50.0, null, 0.80);
    }

    // Alias accessor for tests calling h3Index()
    public String h3Index() {
        return h3CellId;
    }
}