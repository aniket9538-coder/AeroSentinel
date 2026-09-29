package com.aerosentinel.forecast;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Public REST Controller for F4 Multi-Horizon PM2.5 Forecasts.
 *
 * Endpoints:
 * - GET  /api/v1/forecast/{h3Index}            : Retrieves persisted forecast for an H3 cell.
 * - POST /api/v1/forecast/generate             : Generates and persists forecast for an F3 parent prediction.
 * - GET  /api/v1/forecast/parent/{parentId}    : Retrieves persisted forecast by parent prediction UUID.
 */
@RestController
@RequestMapping("/api/v1/forecast")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * Retrieves the latest persisted forecast for a specific H3 cell.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @return ForecastResponse if found (200 OK), or NO_DATA (404 NOT FOUND)
     */
    @GetMapping("/{h3Index}")
    public ResponseEntity<?> getForecastByH3(@PathVariable String h3Index) {
        return forecastService.getForecastByH3(h3Index)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "NO_DATA",
                                "message", "No forecast data found for H3 cell: " + h3Index,
                                "h3Index", h3Index
                        )));
    }

    /**
     * Explicit forecast generation endpoint.
     * Triggers feature extraction -> P3 CLI inference -> database persistence.
     *
     * @param request ForecastGenerateRequest containing parentPredictionId
     * @return Newly generated and persisted ForecastResponse
     */
    @PostMapping("/generate")
    public ResponseEntity<ForecastResponse> generateForecast(
            @Valid @RequestBody ForecastGenerateRequest request
    ) {
        ForecastResponse response = forecastService.generateForecast(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves persisted forecasts for a specific F3 parent prediction UUID.
     */
    @GetMapping("/parent/{parentPredictionId}")
    public ResponseEntity<?> getForecastByParentId(@PathVariable UUID parentPredictionId) {
        return forecastService.getForecastByParentPredictionId(parentPredictionId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "NO_DATA",
                                "message", "No forecast data found for parent prediction: " + parentPredictionId,
                                "parentPredictionId", parentPredictionId.toString()
                        )));
    }
}
