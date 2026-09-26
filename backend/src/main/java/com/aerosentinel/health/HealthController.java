package com.aerosentinel.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final String SERVICE_NAME = "aerosentinel-backend";
    private static final String STATUS_UP = "UP";

    @GetMapping
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = new HealthResponse(
                STATUS_UP,
                SERVICE_NAME,
                Instant.now().toString()
        );
        return ResponseEntity.ok(response);
    }
}
