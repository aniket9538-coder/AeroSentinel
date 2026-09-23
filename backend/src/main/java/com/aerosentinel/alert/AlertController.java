package com.aerosentinel.alert;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAlerts(
            @RequestParam(required = false) UUID cityId,
            @RequestParam(defaultValue = "OPEN") String status
    ) {
        if (cityId != null) {
            return ResponseEntity.ok(alertService.getAlertsByCity(cityId, status));
        }
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlert(@PathVariable UUID alertId) {
        return alertService.acknowledgeAlert(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Alert> createAlert(@RequestBody Alert alert) {
        return ResponseEntity.ok(alertService.createAlert(alert));
    }
}
