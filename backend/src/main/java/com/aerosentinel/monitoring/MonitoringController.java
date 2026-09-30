package com.aerosentinel.monitoring;

import com.aerosentinel.monitoring.dto.MonitoringCoverageResponse;
import com.aerosentinel.monitoring.dto.MonitoringPriorityResponse;
import com.aerosentinel.monitoring.dto.MonitoringRecommendationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for F8 monitoring coverage, priority intelligence, and recommendation decision support.
 */
@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    // Default city: Pune
    private static final UUID DEFAULT_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Retrieves the monitoring coverage status for a specific H3 cell and city (F8-P2).
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @param cityId  Optional city UUID. Defaults to Pune if omitted.
     * @return MonitoringCoverageResponse containing nearest station details, distance, and gap flag
     */
    @GetMapping("/coverage/{h3Index}")
    public ResponseEntity<MonitoringCoverageResponse> getMonitoringCoverage(
            @PathVariable String h3Index,
            @RequestParam(required = false) UUID cityId
    ) {
        UUID effectiveCityId = (cityId != null) ? cityId : DEFAULT_CITY_ID;
        MonitoringCoverageResponse response = monitoringService.getMonitoringCoverage(h3Index, effectiveCityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Computes the deterministic monitoring priority score and classification for an H3 cell (F8-P3).
     * Combines F3 risk, F4 forecast uncertainty interval, and F8-P2 station proximity.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @param cityId  Optional city UUID. Defaults to Pune if omitted.
     * @return MonitoringPriorityResponse containing audited inputs, normalized features, and priority classification
     */
    @GetMapping("/priority/{h3Index}")
    public ResponseEntity<MonitoringPriorityResponse> getMonitoringPriority(
            @PathVariable String h3Index,
            @RequestParam(required = false) UUID cityId
    ) {
        UUID effectiveCityId = (cityId != null) ? cityId : DEFAULT_CITY_ID;
        MonitoringPriorityResponse response = monitoringService.getMonitoringPriority(h3Index, effectiveCityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves monitoring recommendations for all observed H3 cells across a city (F8-P4).
     * Orders cells deterministically by priorityScorePercent descending (with h3Index tie-breaker).
     *
     * @param cityId City UUID
     * @return List of monitoring recommendations for observed cells in the city
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<MonitoringRecommendationResponse>> getCityRecommendations(
            @RequestParam UUID cityId
    ) {
        List<MonitoringRecommendationResponse> response = monitoringService.getCityRecommendations(cityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Computes the actionable monitoring recommendation for an individual H3 cell (F8-P4).
     * Reuses the P3 priority evaluation and produces non-alarmist decision support guidance.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @param cityId  Optional city UUID. Defaults to Pune if omitted.
     * @return MonitoringRecommendationResponse containing priority factors, recommendation type, and rationale
     */
    @GetMapping("/recommendations/{h3Index}")
    public ResponseEntity<MonitoringRecommendationResponse> getMonitoringRecommendation(
            @PathVariable String h3Index,
            @RequestParam(required = false) UUID cityId
    ) {
        UUID effectiveCityId = (cityId != null) ? cityId : DEFAULT_CITY_ID;
        MonitoringRecommendationResponse response = monitoringService.getRecommendation(h3Index, effectiveCityId);
        return ResponseEntity.ok(response);
    }
}
