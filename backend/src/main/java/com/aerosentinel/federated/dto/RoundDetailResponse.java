package com.aerosentinel.federated.dto;

import java.time.Instant;
import java.util.List;

public record RoundDetailResponse(
        String roundId,
        String baseModelVersion,
        String targetModelVersion,
        String status,
        int minQuorum,
        int receivedUpdatesCount,
        List<String> participatingNodes,
        Integer totalSamples,
        EvaluationMetricsDto aggregatedMetrics,
        String failureReason,
        Instant createdAt,
        Instant completedAt,
        List<UpdateResponse> updates
) {}
