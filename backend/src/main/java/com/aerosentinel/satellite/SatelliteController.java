package com.aerosentinel.satellite;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/satellite")
public class SatelliteController {

    private final SatelliteService satelliteService;

    public SatelliteController(SatelliteService satelliteService) {
        this.satelliteService = satelliteService;
    }

    @GetMapping
    public ResponseEntity<List<SatelliteObservation>> getObservations(@RequestParam UUID cityId) {
        return ResponseEntity.ok(satelliteService.getObservationsByCity(cityId));
    }
}
