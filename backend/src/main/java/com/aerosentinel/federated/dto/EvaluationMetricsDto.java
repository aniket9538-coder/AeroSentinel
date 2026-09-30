package com.aerosentinel.federated.dto;

public record EvaluationMetricsDto(
        Double mae,
        Double rmse,
        Double rocAuc,
        Double brierScore
) {}
