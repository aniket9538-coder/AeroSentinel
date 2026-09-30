package com.aerosentinel.federated.dto;

import java.time.Instant;

public record AggregationResponse(
        String roundId,
        String status,
        String baseModelVersion,
        String globalModelVersion,
        int participatingNodesCount,
        int totalSamples,
        EvaluationMetricsDto aggregatedMetrics,
        String artifactPath,
        Instant completedAt
) {}
