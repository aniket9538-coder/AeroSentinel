package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.ModelUpdateResponse;
import com.aerosentinel.dto.federated.SubmitModelUpdateRequest;
import com.aerosentinel.federated.dto.EvaluationMetricsDto;
import com.aerosentinel.federated.dto.NodeUpdateDto;
import com.aerosentinel.federated.dto.UpdateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/federated/rounds/{roundId}/updates")
@CrossOrigin(origins = "*")
public class ModelUpdateController {

    private final ModelUpdateService modelUpdateService;
    private final FederatedRoundService roundService;

    public ModelUpdateController(ModelUpdateService modelUpdateService, FederatedRoundService roundService) {
        this.modelUpdateService = modelUpdateService;
        this.roundService = roundService;
    }

    /**
     * POST /api/v1/federated/rounds/{roundId}/updates
     * Submits and validates a municipal model parameter and metrics update.
     */
    @PostMapping
    public ResponseEntity<?> submitUpdate(
            @PathVariable String roundId,
            @RequestBody SubmitModelUpdateRequest request
    ) {
        // Also sync with roundService for in-memory & round status tracking
        try {
            EvaluationMetricsDto evalDto = null;
            if (request.metrics() != null) {
                evalDto = new EvaluationMetricsDto(
                        request.metrics().get("mae"),
                        request.metrics().get("rmse"),
                        request.metrics().get("rocAuc"),
                        request.metrics().get("brierScore")
                );
            }
            NodeUpdateDto nodeDto = new NodeUpdateDto(
                    request.nodeId(),
                    request.nodeId(),
                    request.localModelVersion(),
                    request.baseModelVersion(),
                    request.sampleCount(),
                    request.weights(),
                    evalDto,
                    request.artifactReference()
            );
            roundService.submitNodeUpdate(roundId, nodeDto);
        } catch (Exception e) {
            // Ignore if round already captured, or propagate if validation
            if (e instanceof RuntimeException) {
                // If validation or conflict exception, let it propagate
                if (e instanceof com.aerosentinel.exception.ValidationException || e instanceof IllegalStateException) {
                    throw e;
                }
            }
        }

        ModelUpdateResponse response = modelUpdateService.submitUpdate(roundId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/federated/rounds/{roundId}/updates
     * Lists all validated model updates submitted for this round.
     */
    @GetMapping
    public ResponseEntity<List<ModelUpdateResponse>> getUpdates(@PathVariable String roundId) {
        return ResponseEntity.ok(modelUpdateService.getUpdatesForRound(roundId));
    }
}
