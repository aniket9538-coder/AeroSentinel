package com.aerosentinel.federated;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "federated_nodes")
public class FederatedNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "node_id", nullable = false, unique = true, length = 50)
    private String nodeId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NodeStatus status = NodeStatus.ONLINE;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion = "global-v1";

    @Column(name = "last_seen_at")
    private Instant lastSeenAt = Instant.now();

    @Column(name = "endpoint_url", length = 255)
    private String endpointUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public FederatedNode() {}

    public FederatedNode(String nodeId, String nodeName, UUID cityId, NodeStatus status, String modelVersion) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.cityId = cityId;
        this.status = status != null ? status : NodeStatus.ONLINE;
        this.modelVersion = modelVersion != null ? modelVersion : "global-v1";
        this.lastSeenAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public FederatedNode(String nodeId, String nodeName, UUID cityId, String status, String modelVersion) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.cityId = cityId;
        this.status = status != null ? parseStatus(status) : NodeStatus.ONLINE;
        this.modelVersion = modelVersion != null ? modelVersion : "global-v1";
        this.lastSeenAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    private static NodeStatus parseStatus(String val) {
        try {
            return NodeStatus.valueOf(val.toUpperCase());
        } catch (Exception e) {
            return NodeStatus.ONLINE;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public void setStatus(String statusStr) { this.status = parseStatus(statusStr); }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
