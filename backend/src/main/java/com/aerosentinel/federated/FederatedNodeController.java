package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.FederatedNodeResponse;
import com.aerosentinel.dto.federated.NodeHeartbeatRequest;
import com.aerosentinel.dto.federated.RegisterNodeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/federated/nodes")
@CrossOrigin(origins = "*")
public class FederatedNodeController {

    private final FederatedNodeService nodeService;

    public FederatedNodeController(FederatedNodeService nodeService) {
        this.nodeService = nodeService;
    }

    /**
     * POST /api/v1/federated/nodes/register
     * Registers a new municipal node or updates existing node endpoint.
     */
    @PostMapping("/register")
    public ResponseEntity<FederatedNodeResponse> registerNode(@RequestBody RegisterNodeRequest request) {
        FederatedNodeResponse response = nodeService.registerNode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/federated/nodes
     * Lists all registered municipal nodes across Pune, Mumbai, Delhi.
     */
    @GetMapping
    public ResponseEntity<List<FederatedNodeResponse>> getAllNodes() {
        return ResponseEntity.ok(nodeService.getAllNodes());
    }

    /**
     * GET /api/v1/federated/nodes/{nodeId}
     * Retrieves health, model version, and endpoint for a specific node.
     */
    @GetMapping("/{nodeId}")
    public ResponseEntity<FederatedNodeResponse> getNode(@PathVariable String nodeId) {
        return ResponseEntity.ok(nodeService.getNode(nodeId));
    }

    /**
     * POST /api/v1/federated/nodes/{nodeId}/heartbeat
     * Updates node liveness, status, and local model version.
     */
    @PostMapping("/{nodeId}/heartbeat")
    public ResponseEntity<FederatedNodeResponse> recordHeartbeat(
            @PathVariable String nodeId,
            @RequestBody(required = false) NodeHeartbeatRequest request
    ) {
        return ResponseEntity.ok(nodeService.recordHeartbeat(nodeId, request));
    }
}
