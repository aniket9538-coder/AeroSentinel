package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.GlobalModelResponse;
import com.aerosentinel.dto.federated.ModelCatalogItemResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/federated/models")
@CrossOrigin(origins = "*")
public class GlobalModelController {

    private final GlobalModelService globalModelService;

    public GlobalModelController(GlobalModelService globalModelService) {
        this.globalModelService = globalModelService;
    }

    /**
     * GET /api/v1/federated/models/active
     * Resolves the active consensus global model and performance metrics.
     */
    @GetMapping("/active")
    public ResponseEntity<GlobalModelResponse> getActiveModel() {
        return ResponseEntity.ok(globalModelService.getActiveModel());
    }

    /**
     * GET /api/v1/federated/models/catalog
     * Retrieves the entire lineage catalog of global models.
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<ModelCatalogItemResponse>> getModelCatalog() {
        return ResponseEntity.ok(globalModelService.getCatalog());
    }

    /**
     * GET /api/v1/federated/models/{modelVersion}
     * Retrieves metadata for a specific global model version.
     */
    @GetMapping("/{modelVersion}")
    public ResponseEntity<GlobalModelResponse> getModelByVersion(@PathVariable String modelVersion) {
        return ResponseEntity.ok(globalModelService.getModelByVersion(modelVersion));
    }
}
