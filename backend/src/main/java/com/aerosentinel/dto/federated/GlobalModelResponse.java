package com.aerosentinel.dto.federated;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GlobalModelResponse(
    String modelVersion,
    String baseModelVersion,
    String roundId,
    boolean isActive,
    List<String> participatingNodes,
    int totalSamples,
    Map<String, Double> metrics,
    String artifactPath,
    Instant createdAt
) {}
