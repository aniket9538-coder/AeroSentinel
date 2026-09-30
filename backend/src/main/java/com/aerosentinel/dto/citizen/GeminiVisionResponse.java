package com.aerosentinel.dto.citizen;

import java.time.Instant;
import java.util.List;

public record GeminiVisionResponse(
        String modelName,
        String detectedCategory,
        String visibleCondition,
        Double confidence,
        List<String> visualIndicators,
        String narrativeSummary,
        Boolean verificationRequired,
        String limitations,
        Instant analyzedAt,
        String analysisStatus
) {}