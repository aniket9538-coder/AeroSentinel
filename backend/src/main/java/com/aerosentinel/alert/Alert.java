package com.aerosentinel.alert;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "grid_cell_id", nullable = false)
    private UUID gridCellId;

    @Column(name = "h3_index", length = 30)
    private String h3Index;

    @Column(name = "prediction_id")
    private UUID predictionId;

    @Column(name = "event_code", length = 50)
    private String eventCode;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "evidence_score")
    private Double evidenceScore;

    @Column(name = "triage_state", length = 30)
    private String triageState = "ALERT_CANDIDATE";

    @Column(length = 30)
    private String consistency;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "forecast_summary", columnDefinition = "TEXT")
    private String forecastSummary;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "generated_by", length = 50)
    private String generatedBy = "F5_EVIDENCE_ENGINE";

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    public Alert() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public UUID getGridCellId() { return gridCellId; }
    public void setGridCellId(UUID gridCellId) { this.gridCellId = gridCellId; }
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public UUID getPredictionId() { return predictionId; }
    public void setPredictionId(UUID predictionId) { this.predictionId = predictionId; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public Double getEvidenceScore() { return evidenceScore; }
    public void setEvidenceScore(Double evidenceScore) { this.evidenceScore = evidenceScore; }
    public String getTriageState() { return triageState; }
    public void setTriageState(String triageState) { this.triageState = triageState; }
    public String getConsistency() { return consistency; }
    public void setConsistency(String consistency) { this.consistency = consistency; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getForecastSummary() { return forecastSummary; }
    public void setForecastSummary(String forecastSummary) { this.forecastSummary = forecastSummary; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public UUID getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(UUID acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getAlertId() { return id != null ? id.toString() : null; }
    public String getH3CellId() { return h3Index; }
    public Double getConfidence() { return 0.75; }
    public String getDescription() { return message; }
    public Double getExpectedSpike() { return null; }
    public String getEvidenceSummary() { return forecastSummary; }
    public Instant getClosedAt() { return resolvedAt; }
    public void setClosedAt(Instant closedAt) { this.resolvedAt = closedAt; }
}
