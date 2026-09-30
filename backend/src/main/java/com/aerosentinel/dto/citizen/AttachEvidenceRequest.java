package com.aerosentinel.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AttachEvidenceRequest(
        @NotBlank(message = "evidenceType is required (AIR, WEATHER, FIRE, SATELLITE, CITIZEN, FORECAST)")
        String evidenceType,

        String sourceId,

        @NotBlank(message = "valueSummary is required")
        String valueSummary,

        Double strength,

        @NotNull(message = "observedAt is required")
        Instant observedAt
) {}