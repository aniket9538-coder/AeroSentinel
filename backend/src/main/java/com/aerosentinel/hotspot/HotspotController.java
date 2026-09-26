package com.aerosentinel.hotspot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public REST Controller for Hotspot Detection & Spatial Risk Intelligence.
 * Exposes clean, aggregate and cell-level risk scores without leaking internal feature tables.
 */
@RestController
@RequestMapping("/api/v1/hotspots")
public class HotspotController {

    private final HotspotService hotspotService;

    // Default city: Pune
    private static final UUID DEFAULT_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    /**
     * Retrieves the latest hotspot risk overview for all H3 cells in the city.
     *
     * @param cityId Optional city UUID. Defaults to Pune if omitted.
     * @return HotspotOverviewResponse with cell-level risk scores and freshness
     */
    @GetMapping
    public ResponseEntity<HotspotOverviewResponse> getHotspots(
            @RequestParam(required = false) UUID cityId
    ) {
        UUID effectiveCityId = (cityId != null) ? cityId : DEFAULT_CITY_ID;
        HotspotOverviewResponse response = hotspotService.getHotspotsForCity(effectiveCityId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the latest hotspot risk prediction for a specific H3 cell.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @return HotspotCellDto if found, or 404 NOT FOUND
     */
    @GetMapping("/{h3Index}")
    public ResponseEntity<HotspotCellDto> getHotspotByH3(
            @PathVariable String h3Index
    ) {
        return hotspotService.getHotspotByH3(h3Index)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the rich spatial context for a specific H3 cell, serving as the integration anchor
     * for future F4 Forecast and F5 Evidence models.
     *
     * @param h3Index 15-character Uber H3 cell identifier
     * @return HotspotSpatialContext if found, or 404 NOT FOUND
     */
    @GetMapping("/{h3Index}/context")
    public ResponseEntity<HotspotSpatialContext> getHotspotContext(
            @PathVariable String h3Index
    ) {
        return hotspotService.getSpatialContextForH3(h3Index)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
