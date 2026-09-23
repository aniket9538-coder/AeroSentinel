package com.aerosentinel.sensor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sensors")
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public ResponseEntity<List<MonitoringStation>> getStations(@RequestParam(required = false) UUID cityId) {
        if (cityId != null) {
            return ResponseEntity.ok(sensorService.getStationsByCity(cityId));
        }
        return ResponseEntity.ok(sensorService.getAllStations());
    }
}
