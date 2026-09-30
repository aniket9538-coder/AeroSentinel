package com.aerosentinel.dto.federated;

import java.util.List;
import java.util.Map;

public record SubmitModelUpdateRequest(
    String nodeId,
    String baseModelVersion,
    String localModelVersion,
    int sampleCount,
    Map<String, Double> metrics,
    List<Double> weights,
    String artifactReference
) {}
