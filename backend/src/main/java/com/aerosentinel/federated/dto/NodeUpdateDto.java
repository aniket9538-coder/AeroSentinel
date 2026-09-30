package com.aerosentinel.federated.dto;

import java.util.List;

public record NodeUpdateDto(
        String cityName,
        String nodeId,
        String localModelVersion,
        String baseModelVersion,
        int sampleCount,
        List<Double> weights,
        EvaluationMetricsDto metrics,
        String artifactReference
) {
    public String getEffectiveCityName() {
        if (cityName != null && !cityName.isBlank()) {
            return cityName.trim().toUpperCase();
        }
        if (nodeId != null && !nodeId.isBlank()) {
            return nodeId.trim().toUpperCase();
        }
        return "UNKNOWN_CITY";
    }
}
