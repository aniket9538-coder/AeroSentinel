package com.aerosentinel.federated;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "federated_global_models")
public class FederatedGlobalModel {

    @Id
    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "base_model_version", length = 50)
    private String baseModelVersion;

    @Column(name = "round_id", length = 50)
    private String roundId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "total_samples")
    private Integer totalSamples = 0;

    @Column(name = "participating_nodes", columnDefinition = "TEXT")
    private String participatingNodes = "[]";

    @Column(name = "weights_json", columnDefinition = "TEXT")
    private String weightsJson;

    @Column(name = "mae")
    private Double mae;

    @Column(name = "rmse")
    private Double rmse;

    @Column(name = "roc_auc")
    private Double rocAuc;

    @Column(name = "brier_score")
    private Double brierScore;

    @Column(name = "artifact_path", nullable = false, length = 255)
    private String artifactPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public FederatedGlobalModel() {}

    public FederatedGlobalModel(
            String version,
            String baseModelVersion,
            String roundId,
            boolean isActive,
            Integer totalSamples,
            String participatingNodes,
            String weightsJson,
            Double mae,
            Double rmse,
            Double rocAuc,
            Double brierScore,
            String artifactPath
    ) {
        this.version = version;
        this.baseModelVersion = baseModelVersion;
        this.roundId = roundId;
        this.isActive = isActive;
        this.totalSamples = totalSamples;
        this.participatingNodes = participatingNodes;
        this.weightsJson = weightsJson;
        this.mae = mae;
        this.rmse = rmse;
        this.rocAuc = rocAuc;
        this.brierScore = brierScore;
        this.artifactPath = artifactPath;
        this.createdAt = Instant.now();
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getBaseModelVersion() { return baseModelVersion; }
    public void setBaseModelVersion(String baseModelVersion) { this.baseModelVersion = baseModelVersion; }

    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Integer getTotalSamples() { return totalSamples; }
    public void setTotalSamples(Integer totalSamples) { this.totalSamples = totalSamples; }

    public String getParticipatingNodes() { return participatingNodes; }
    public void setParticipatingNodes(String participatingNodes) { this.participatingNodes = participatingNodes; }

    public String getWeightsJson() { return weightsJson; }
    public void setWeightsJson(String weightsJson) { this.weightsJson = weightsJson; }

    public Double getMae() { return mae; }
    public void setMae(Double mae) { this.mae = mae; }

    public Double getRmse() { return rmse; }
    public void setRmse(Double rmse) { this.rmse = rmse; }

    public Double getRocAuc() { return rocAuc; }
    public void setRocAuc(Double rocAuc) { this.rocAuc = rocAuc; }

    public Double getBrierScore() { return brierScore; }
    public void setBrierScore(Double brierScore) { this.brierScore = brierScore; }

    public String getArtifactPath() { return artifactPath; }
    public void setArtifactPath(String artifactPath) { this.artifactPath = artifactPath; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
