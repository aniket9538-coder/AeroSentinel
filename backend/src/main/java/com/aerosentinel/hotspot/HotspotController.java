package com.aerosentinel.hotspot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hotspots")
public class HotspotController {

    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    @GetMapping
    public ResponseEntity<List<HotspotPrediction>> getHotspots(@RequestParam(required = false) UUID cellId) {
        if (cellId != null) {
            return ResponseEntity.ok(hotspotService.getHotspotsByCell(cellId));
        }
        return ResponseEntity.ok(hotspotService.getAllHotspots());
    }
}
