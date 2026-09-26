package com.aerosentinel.grid;

import com.aerosentinel.dto.grid.GridCellObservationResponse;
import com.aerosentinel.dto.grid.GridCellResponse;
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
    public ResponseEntity<List<GridCellResponse>> getCells(@RequestParam UUID cityId) {
        return ResponseEntity.ok(gridService.getGridCellsForCity(cityId));
    }

    @GetMapping("/{h3Index}")
    public ResponseEntity<GridCellResponse> getCell(@PathVariable String h3Index) {
        return ResponseEntity.ok(gridService.getGridCellByH3(h3Index));
    }

    @GetMapping("/{h3Index}/observations")
    public ResponseEntity<GridCellObservationResponse> getCellObservations(@PathVariable String h3Index) {
        return ResponseEntity.ok(gridService.getCellObservations(h3Index));
    }
}
