package com.aerosentinel.forecast;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for multi-horizon PM2.5 forecasts.
 * Persists independent horizon evaluations (1h, 3h, 6h) with empirical residual intervals
 * and strict lineage to F3 hotspot predictions and F2 feature snapshots.
 */
@Entity
@Table(name = "forecasts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_forecast_parent_horizon", columnNames = {"parent_prediction_id", "horizon_hours"})
})
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "parent_prediction_id", nullable = false)
    private UUID parentPredictionId;

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "h3_index", nullable = false, length = 30)
    private String h3Index;

    @Column(name = "grid_cell_id")
    private UUID gridCellId;

    @Column(name = "feature_snapshot_id")
    private UUID featureSnapshotId;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "target_time", nullable = false)
    private Instant targetTime;

    @Column(name = "horizon_hours", nullable = false)
    private Integer horizonHours;

    @Column(name = "predicted_pm25", nullable = false)
    private Double predictedPm25;

    @Column(name = "lower_bound", nullable = false)
    private Double lowerBound;

    @Column(name = "upper_bound", nullable = false)
    private Double upperBound;

    @Column(name = "forecast_confidence")
    private Double forecastConfidence;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion = "forecast_regressors_v1";

    @Column(name = "unit", nullable = false, length = 20)
    private String unit = "ug/m3";

    @Column(name = "status", nullable = false, length = 30)
    private String status = "SUCCESS";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Forecast() {}

    public Forecast(
            UUID parentPredictionId,
            UUID cityId,
            String h3Index,
            UUID gridCellId,
            UUID featureSnapshotId,
            Instant generatedAt,
            Instant targetTime,
            Integer horizonHours,
            Double predictedPm25,
            Double lowerBound,
            Double upperBound,
            Double forecastConfidence,
            String modelVersion,
            String unit,
            String status
    ) {
        this.parentPredictionId = parentPredictionId;
        this.cityId = cityId;
        this.h3Index = h3Index;
        this.gridCellId = gridCellId;
        this.featureSnapshotId = featureSnapshotId;
        this.generatedAt = generatedAt;
        this.targetTime = targetTime;
        this.horizonHours = horizonHours;
        this.predictedPm25 = predictedPm25;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.forecastConfidence = forecastConfidence;
        this.modelVersion = modelVersion != null ? modelVersion : "forecast_regressors_v1";
        this.unit = unit != null ? unit : "ug/m3";
        this.status = status != null ? status : "SUCCESS";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getParentPredictionId() { return parentPredictionId; }
    public void setParentPredictionId(UUID parentPredictionId) { this.parentPredictionId = parentPredictionId; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }

    public UUID getGridCellId() { return gridCellId; }
    public void setGridCellId(UUID gridCellId) { this.gridCellId = gridCellId; }

    public UUID getFeatureSnapshotId() { return featureSnapshotId; }
    public void setFeatureSnapshotId(UUID featureSnapshotId) { this.featureSnapshotId = featureSnapshotId; }

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

    public Double getForecastConfidence() { return forecastConfidence; }
    public void setForecastConfidence(Double forecastConfidence) { this.forecastConfidence = forecastConfidence; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @PrePersist
    @PreUpdate
    public void validateLineageIntegrity() {
        if ("SUCCESS".equalsIgnoreCase(this.status)) {
            if (this.parentPredictionId == null) {
                throw new IllegalStateException("parentPredictionId cannot be null for SUCCESS forecast");
            }
            if (this.cityId == null) {
                throw new IllegalStateException("cityId cannot be null for SUCCESS forecast");
            }
            if (this.h3Index == null || this.h3Index.isBlank()) {
                throw new IllegalStateException("h3Index cannot be null for SUCCESS forecast");
            }
            if (this.featureSnapshotId == null) {
                throw new IllegalStateException("featureSnapshotId cannot be null for SUCCESS forecast");
            }
        }
    }
}
