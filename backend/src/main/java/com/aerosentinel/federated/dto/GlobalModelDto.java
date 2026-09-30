package com.aerosentinel.federated.dto;

import java.time.Instant;
import java.util.List;

public record GlobalModelDto(
        String version,
        String baseModelVersion,
        String roundId,
        boolean isActive,
        Integer totalSamples,
        List<String> participatingNodes,
        EvaluationMetricsDto metrics,
        String artifactPath,
        Instant createdAt
) {}
