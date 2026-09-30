package com.aerosentinel.federated;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "model_updates")
public class ModelUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "round_id", nullable = false, length = 50)
    private String roundId;

    @Column(name = "node_id", nullable = false, length = 50)
    private String nodeId;

    @Column(name = "base_model_version", nullable = false, length = 50)
    private String baseModelVersion;

    @Column(name = "local_model_version", nullable = false, length = 50)
    private String localModelVersion;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    @Column(name = "mae")
    private Double mae;

    @Column(name = "rmse")
    private Double rmse;

    @Column(name = "roc_auc")
    private Double rocAuc;

    @Column(name = "brier_score")
    private Double brierScore;

    @Column(name = "weights_json", columnDefinition = "TEXT")
    private String weightsJson;

    @Column(name = "artifact_reference", length = 255)
    private String artifactReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UpdateStatus status = UpdateStatus.RECEIVED;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ModelUpdate() {}

    public ModelUpdate(
            String roundId,
            String nodeId,
            String baseModelVersion,
            String localModelVersion,
            int sampleCount,
            Double mae,
            Double rmse,
            Double rocAuc,
            Double brierScore,
            String weightsJson,
            String artifactReference,
            UpdateStatus status
    ) {
        this.roundId = roundId;
        this.nodeId = nodeId;
        this.baseModelVersion = baseModelVersion;
        this.localModelVersion = localModelVersion;
        this.sampleCount = sampleCount;
        this.mae = mae;
        this.rmse = rmse;
        this.rocAuc = rocAuc;
        this.brierScore = brierScore;
        this.weightsJson = weightsJson;
        this.artifactReference = artifactReference;
        this.status = status != null ? status : UpdateStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getBaseModelVersion() { return baseModelVersion; }
    public void setBaseModelVersion(String baseModelVersion) { this.baseModelVersion = baseModelVersion; }

    public String getLocalModelVersion() { return localModelVersion; }
    public void setLocalModelVersion(String localModelVersion) { this.localModelVersion = localModelVersion; }

    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

    public Double getMae() { return mae; }
    public void setMae(Double mae) { this.mae = mae; }

    public Double getRmse() { return rmse; }
    public void setRmse(Double rmse) { this.rmse = rmse; }

    public Double getRocAuc() { return rocAuc; }
    public void setRocAuc(Double rocAuc) { this.rocAuc = rocAuc; }

    public Double getBrierScore() { return brierScore; }
    public void setBrierScore(Double brierScore) { this.brierScore = brierScore; }

    public String getWeightsJson() { return weightsJson; }
    public void setWeightsJson(String weightsJson) { this.weightsJson = weightsJson; }

    public String getArtifactReference() { return artifactReference; }
    public void setArtifactReference(String artifactReference) { this.artifactReference = artifactReference; }

    public UpdateStatus getStatus() { return status; }
    public void setStatus(UpdateStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
