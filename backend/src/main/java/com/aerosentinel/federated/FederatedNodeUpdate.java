package com.aerosentinel.federated;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "federated_node_updates", uniqueConstraints = {
    @UniqueConstraint(name = "uq_round_city_update", columnNames = {"round_id", "city_name"})
})
public class FederatedNodeUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "update_id")
    private UUID updateId;

    @Column(name = "round_id", nullable = false, length = 50)
    private String roundId;

    @Column(name = "city_name", nullable = false, length = 50)
    private String cityName;

    @Column(name = "local_model_version", nullable = false, length = 50)
    private String localModelVersion;

    @Column(name = "base_model_version", nullable = false, length = 50)
    private String baseModelVersion;

    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

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

    @Column(name = "artifact_reference", length = 255)
    private String artifactReference;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", referencedColumnName = "round_id", insertable = false, updatable = false)
    private FederatedRound round;

    public FederatedNodeUpdate() {}

    public FederatedNodeUpdate(
            String roundId,
            String cityName,
            String localModelVersion,
            String baseModelVersion,
            int sampleCount,
            String weightsJson,
            Double mae,
            Double rmse,
            Double rocAuc,
            Double brierScore,
            String artifactReference
    ) {
        this.roundId = roundId;
        this.cityName = cityName;
        this.localModelVersion = localModelVersion;
        this.baseModelVersion = baseModelVersion;
        this.sampleCount = sampleCount;
        this.weightsJson = weightsJson;
        this.mae = mae;
        this.rmse = rmse;
        this.rocAuc = rocAuc;
        this.brierScore = brierScore;
        this.artifactReference = artifactReference;
        this.submittedAt = Instant.now();
    }

    public UUID getUpdateId() { return updateId; }
    public void setUpdateId(UUID updateId) { this.updateId = updateId; }

    public String getRoundId() { return roundId; }
    public void setRoundId(String roundId) { this.roundId = roundId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getLocalModelVersion() { return localModelVersion; }
    public void setLocalModelVersion(String localModelVersion) { this.localModelVersion = localModelVersion; }

    public String getBaseModelVersion() { return baseModelVersion; }
    public void setBaseModelVersion(String baseModelVersion) { this.baseModelVersion = baseModelVersion; }

    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

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

    public String getArtifactReference() { return artifactReference; }
    public void setArtifactReference(String artifactReference) { this.artifactReference = artifactReference; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public FederatedRound getRound() { return round; }
    public void setRound(FederatedRound round) { this.round = round; }
}
