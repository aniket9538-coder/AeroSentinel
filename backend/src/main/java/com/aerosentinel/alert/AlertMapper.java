package com.aerosentinel.alert;

import com.aerosentinel.dto.alert.AlertResponse;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        if (alert == null) return null;
        return new AlertResponse(
                alert.getAlertId(),
                alert.getEventId(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getH3CellId(),
                alert.getRiskScore(),
                alert.getConfidence(),
                alert.getExpectedSpike(),
                alert.getEvidenceSummary(),
                alert.getRecommendedAction(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getAcknowledgedAt(),
                alert.getClosedAt()
        );
    }
}