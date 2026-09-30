package com.aerosentinel.dto.authority;

import java.time.Instant;
import java.util.UUID;

public record ActionResponse(
        String actionId,
        String eventId,
        String actionType,
        String performedBy,
        String notes,
        String result,
        Instant performedAt
) {
    // Overloaded constructor to support callers passing UUID for eventId and/or performedBy
    public ActionResponse(
            String actionId,
            UUID eventId,
            String actionType,
            UUID performedBy,
            String notes,
            String result,
            Instant performedAt
    ) {
        this(
                actionId,
                eventId != null ? eventId.toString() : null,
                actionType,
                performedBy != null ? performedBy.toString() : null,
                notes,
                result,
                performedAt
        );
    }

    public ActionResponse(
            String actionId,
            UUID eventId,
            String actionType,
            String performedBy,
            String notes,
            String result,
            Instant performedAt
    ) {
        this(
                actionId,
                eventId != null ? eventId.toString() : null,
                actionType,
                performedBy,
                notes,
                result,
                performedAt
        );
    }
}