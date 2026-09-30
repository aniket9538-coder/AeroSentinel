package com.aerosentinel.dto.alert;

import java.util.UUID;

public record EvaluateAlertRequest(
        String eventId,
        String h3Index,
        Double riskScore,
        Double confidence,
        UUID cityId,
        Double expectedSpike,
        String evidenceSummary
) {
    public EvaluateAlertRequest(String eventId, String h3Index, Double riskScore, Double confidence, UUID cityId) {
        this(eventId, h3Index, riskScore, confidence, cityId, null, null);
    }
}