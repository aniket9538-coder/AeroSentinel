package com.aerosentinel.grid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grid")
public class GridController {

    private final GridService gridService;

    public GridController(GridService gridService) {
        this.gridService = gridService;
    }

    @GetMapping
    public ResponseEntity<List<GridCell>> getCells(@RequestParam UUID cityId) {
        return ResponseEntity.ok(gridService.getCellsByCity(cityId));
    }

    @GetMapping("/{h3Index}")
    public ResponseEntity<GridCell> getCell(@PathVariable String h3Index) {
        return gridService.getCellByH3(h3Index)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
