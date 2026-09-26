package com.aerosentinel.sensor;

import com.aerosentinel.air.AirService;
import com.aerosentinel.dto.air.AirQualityHistoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final AirService airService;

    public StationController(AirService airService) {
        this.airService = airService;
    }

    /**
     * F1 Canonical Endpoint: Station historical air quality observations.
     * GET /api/v1/stations/{stationId}/air-quality?from={from}&to={to}
     */
    @GetMapping("/{stationId}/air-quality")
    public ResponseEntity<AirQualityHistoryResponse> getStationAirQuality(
            @PathVariable String stationId,
            @RequestParam(name = "from") String from,
            @RequestParam(name = "to") String to) {
        return ResponseEntity.ok(airService.getStationAirQualityHistory(stationId, from, to));
    }
}
