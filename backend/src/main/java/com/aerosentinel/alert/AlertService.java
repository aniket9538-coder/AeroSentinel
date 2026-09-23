package com.aerosentinel.alert;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> getAlertsByCity(UUID cityId, String status) {
        return alertRepository.findByCityIdAndStatusOrderByCreatedAtDesc(cityId, status);
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<Alert> acknowledgeAlert(UUID alertId) {
        return alertRepository.findById(alertId).map(alert -> {
            alert.setStatus("ACKNOWLEDGED");
            alert.setAcknowledgedAt(Instant.now());
            return alertRepository.save(alert);
        });
    }

    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }
}
