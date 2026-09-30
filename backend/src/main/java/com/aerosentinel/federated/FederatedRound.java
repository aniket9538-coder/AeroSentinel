package com.aerosentinel.federated;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "federated_rounds")
public class FederatedRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "round_id", nullable = false, unique = true, length = 50)
    private String roundId;

    @Column(name = "base_model_version", nullable = false, length = 50)
    private String baseModelVersion;

    @Column(name = "target_model_version", length = 50)
    private String targetModelVersion;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "CREATED";

    @Column(name = "participating_nodes", columnDefinition = "TEXT")
    private String participatingNodes = "[\"PUNE\",\"MUMBAI\",\"DELHI\"]";

    @Column(name = "min_quorum", nullable = false)
    private int minQuorum = 2;

    @Column(name = "total_samples")
    private Integer totalSamples = 0;

    @Column(name = "mae")
    private Double mae;

    @Column(name = "rmse")
    private Double rmse;

    @Column(name = "roc_auc")
    private Double rocAuc;

    @Column(name = "brier_score")
    private Double brierScore;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FederatedNodeUpdate> updates = new ArrayList<>();

    public FederatedRound() {}

    public FederatedRound(String roundId, String baseModelVersion, int minQuorum, String participatingNodes) {
        this.roundId = roundId;
        this.baseModelVersion = baseModelVersion;
        this.minQuorum = minQuorum;
        if (participatingNodes != null) {
            this.participatingNodes = participatingNodes;
        }
        this.status = "CREATED";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }

    public String getBaseModelVersion() { return baseModelVersion; }
    public void setBaseModelVersion(String baseModelVersion) { this.baseModelVersion = baseModelVersion; }

    public String getTargetModelVersion() { return targetModelVersion; }
    public void setTargetModelVersion(String targetModelVersion) { this.targetModelVersion = targetModelVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getParticipatingNodes() { return participatingNodes; }
    public void setParticipatingNodes(String participatingNodes) { this.participatingNodes = participatingNodes; }

    public int getMinQuorum() { return minQuorum; }
    public void setMinQuorum(int minQuorum) { this.minQuorum = minQuorum; }

    public Integer getTotalSamples() { return totalSamples; }
    public void setTotalSamples(Integer totalSamples) { this.totalSamples = totalSamples; }

    public Double getMae() { return mae; }
    public void setMae(Double mae) { this.mae = mae; }

    public Double getRmse() { return rmse; }
    public void setRmse(Double rmse) { this.rmse = rmse; }

    public Double getRocAuc() { return rocAuc; }
    public void setRocAuc(Double rocAuc) { this.rocAuc = rocAuc; }

    public Double getBrierScore() { return brierScore; }
    public void setBrierScore(Double brierScore) { this.brierScore = brierScore; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public List<FederatedNodeUpdate> getUpdates() { return updates; }
    public void setUpdates(List<FederatedNodeUpdate> updates) { this.updates = updates; }
}
