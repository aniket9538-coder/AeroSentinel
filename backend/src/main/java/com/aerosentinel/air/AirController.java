package com.aerosentinel.air;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/air")
public class AirController {

    private final AirService airService;

    public AirController(AirService airService) {
        this.airService = airService;
    }

    @GetMapping("/current")
    public ResponseEntity<List<AirObservation>> getCurrentObservations(@RequestParam UUID cityId) {
        return ResponseEntity.ok(airService.getCurrentAirObservations(cityId));
    }

    @GetMapping("/observations")
    public ResponseEntity<List<AirObservation>> getStationObservations(@RequestParam String stationId) {
        return ResponseEntity.ok(airService.getStationObservations(stationId));
    }
}
