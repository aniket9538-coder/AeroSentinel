package com.aerosentinel.forecast;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for triggering forecast generation for an existing F3 parent prediction.
 */
public record ForecastGenerateRequest(
        @NotNull(message = "parentPredictionId is required")
        UUID parentPredictionId,

        UUID cityId,

        String h3Index
) {}
