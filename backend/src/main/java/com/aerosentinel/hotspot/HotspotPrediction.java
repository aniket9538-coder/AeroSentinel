package com.aerosentinel.hotspot;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hotspot_predictions")
public class HotspotPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "grid_cell_id")
    private UUID gridCellId;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(name = "h3_index", length = 30)
    private String h3Index;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "feature_snapshot_id")
    private UUID featureSnapshotId;

    @Column(name = "explanation_status", length = 50)
    private String explanationStatus = "PENDING";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public HotspotPrediction() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getGridCellId() { return gridCellId; }
    public void setGridCellId(UUID gridCellId) { this.gridCellId = gridCellId; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }

    public Instant getPredictedAt() { return predictedAt; }
    public void setPredictedAt(Instant predictedAt) { this.predictedAt = predictedAt; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public UUID getFeatureSnapshotId() { return featureSnapshotId; }
    public void setFeatureSnapshotId(UUID featureSnapshotId) { this.featureSnapshotId = featureSnapshotId; }

    public String getExplanationStatus() { return explanationStatus; }
    public void setExplanationStatus(String explanationStatus) { this.explanationStatus = explanationStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
