package com.aerosentinel.forecast;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping("/{cellId}")
    public ResponseEntity<List<Forecast>> getForecasts(@PathVariable UUID cellId) {
        return ResponseEntity.ok(forecastService.getForecastsByCell(cellId));
    }
}
