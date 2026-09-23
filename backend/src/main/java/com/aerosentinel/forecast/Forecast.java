package com.aerosentinel.forecast;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "forecasts")
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "grid_cell_id", nullable = false)
    private UUID gridCellId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "target_time", nullable = false)
    private Instant targetTime;

    @Column(name = "horizon_hours", nullable = false)
    private Integer horizonHours;

    @Column(name = "predicted_pm25", nullable = false)
    private Double predictedPm25;

    @Column(name = "lower_bound")
    private Double lowerBound;

    @Column(name = "upper_bound")
    private Double upperBound;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Forecast() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGridCellId() { return gridCellId; }
    public void setGridCellId(UUID gridCellId) { this.gridCellId = gridCellId; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public Instant getTargetTime() { return targetTime; }
    public void setTargetTime(Instant targetTime) { this.targetTime = targetTime; }
    public Integer getHorizonHours() { return horizonHours; }
    public void setHorizonHours(Integer horizonHours) { this.horizonHours = horizonHours; }
    public Double getPredictedPm25() { return predictedPm25; }
    public void setPredictedPm25(Double predictedPm25) { this.predictedPm25 = predictedPm25; }
    public Double getLowerBound() { return lowerBound; }
    public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }
    public Double getUpperBound() { return upperBound; }
    public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
