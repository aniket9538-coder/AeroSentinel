package com.aerosentinel.dto.inspection;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AssignTeamRequest(
        @NotNull UUID teamId,
        UUID assignedBy,
        Instant scheduledAt,
        String notes
) {}
