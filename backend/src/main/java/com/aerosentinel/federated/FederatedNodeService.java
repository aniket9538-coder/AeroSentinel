package com.aerosentinel.federated;

import com.aerosentinel.dto.federated.FederatedNodeResponse;
import com.aerosentinel.dto.federated.NodeHeartbeatRequest;
import com.aerosentinel.dto.federated.RegisterNodeRequest;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FederatedNodeService {

    private static final Logger log = LoggerFactory.getLogger(FederatedNodeService.class);

    private final FederatedNodeRepository nodeRepository;

    public FederatedNodeService(FederatedNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public FederatedNodeResponse registerNode(RegisterNodeRequest request) {
        if (request == null || request.nodeId() == null || request.nodeId().trim().isEmpty()) {
            throw new ValidationException("nodeId is required for registration");
        }
        String nodeId = request.nodeId().trim().toUpperCase();
        String nodeName = (request.nodeName() != null && !request.nodeName().trim().isEmpty())
                ? request.nodeName().trim()
                : nodeId + " Municipal Environmental Node";

        FederatedNode node = nodeRepository.findByNodeId(nodeId)
                .orElseGet(() -> {
                    FederatedNode newNode = new FederatedNode();
                    newNode.setNodeId(nodeId);
                    newNode.setStatus(NodeStatus.REGISTERED);
                    return newNode;
                });

        node.setNodeName(nodeName);
        if (request.cityId() != null) {
            node.setCityId(request.cityId());
        }
        if (request.endpointUrl() != null) {
            node.setEndpointUrl(request.endpointUrl());
        }
        node.setLastSeenAt(Instant.now());
        node.setUpdatedAt(Instant.now());

        FederatedNode saved = nodeRepository.save(node);
        log.info("FEDERATED_NODE_REGISTERED nodeId={} status={} endpoint={}",
                saved.getNodeId(), saved.getStatus(), saved.getEndpointUrl());

        return toResponse(saved);
    }

    @Transactional
    public FederatedNodeResponse recordHeartbeat(String nodeId, NodeHeartbeatRequest request) {
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new ValidationException("nodeId is required for heartbeat");
        }
        FederatedNode node = nodeRepository.findByNodeId(nodeId.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Federated node not found: " + nodeId));

        node.setLastSeenAt(Instant.now());
        node.setUpdatedAt(Instant.now());

        if (request != null) {
            if (request.status() != null && !request.status().trim().isEmpty()) {
                try {
                    node.setStatus(NodeStatus.valueOf(request.status().trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid NodeStatus received in heartbeat: {}", request.status());
                }
            } else {
                // If it was OFFLINE and sent a heartbeat, mark ONLINE
                if (node.getStatus() == NodeStatus.OFFLINE) {
                    node.setStatus(NodeStatus.ONLINE);
                }
            }

            if (request.modelVersion() != null && !request.modelVersion().trim().isEmpty()) {
                node.setModelVersion(request.modelVersion().trim());
            }
        }

        FederatedNode saved = nodeRepository.save(node);
        log.debug("FEDERATED_NODE_HEARTBEAT nodeId={} status={} modelVersion={}",
                saved.getNodeId(), saved.getStatus(), saved.getModelVersion());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FederatedNodeResponse> getAllNodes() {
        return nodeRepository.findAllByOrderByNodeIdAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FederatedNodeResponse getNode(String nodeId) {
        return nodeRepository.findByNodeId(nodeId.trim().toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Federated node not found: " + nodeId));
    }

    @Transactional
    public FederatedNodeResponse updateNodeStatus(String nodeId, NodeStatus status) {
        FederatedNode node = nodeRepository.findByNodeId(nodeId.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Federated node not found: " + nodeId));
        node.setStatus(status);
        node.setUpdatedAt(Instant.now());
        FederatedNode saved = nodeRepository.save(node);
        return toResponse(saved);
    }

    @Transactional
    public List<FederatedNodeResponse> checkOfflineNodes(Duration threshold) {
        Instant cutoff = Instant.now().minus(threshold);
        List<FederatedNode> nodes = nodeRepository.findAll();
        for (FederatedNode node : nodes) {
            if (node.getStatus() != NodeStatus.OFFLINE && node.getLastSeenAt() != null && node.getLastSeenAt().isBefore(cutoff)) {
                node.setStatus(NodeStatus.OFFLINE);
                node.setUpdatedAt(Instant.now());
                nodeRepository.save(node);
                log.info("FEDERATED_NODE_OFFLINE nodeId={} lastSeenAt={}", node.getNodeId(), node.getLastSeenAt());
            }
        }
        return getAllNodes();
    }

    private FederatedNodeResponse toResponse(FederatedNode node) {
        return new FederatedNodeResponse(
                node.getNodeId(),
                node.getNodeName(),
                node.getCityId(),
                node.getStatus() != null ? node.getStatus().name() : "OFFLINE",
                node.getModelVersion(),
                node.getLastSeenAt(),
                node.getEndpointUrl()
        );
    }
}
