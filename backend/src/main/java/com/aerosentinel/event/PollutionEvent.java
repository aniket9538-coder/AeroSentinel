package com.aerosentinel.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pollution_events")
public class PollutionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "grid_cell_id", nullable = false)
    private UUID gridCellId;

    @Column(name = "h3_index", length = 30)
    private String h3Index;

    @Column(name = "prediction_id")
    private UUID predictionId;

    @Column(name = "event_code", unique = true, nullable = false, length = 50)
    private String eventCode;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public PollutionEvent() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getEventId() { return eventCode != null ? eventCode : (id != null ? id.toString() : null); }
    public void setEventId(String eventId) { this.eventCode = eventId; }
    public Double getRiskScore() { return 50.0; }
    public void setRiskScore(Double riskScore) {}
    public String getRiskLevel() { return severity != null ? severity : "MODERATE"; }
    public Double getConfidence() { return 0.75; }
    public void setConfidence(Double confidence) {}
    public UUID getCityId() { return null; }
    public void setCityId(UUID cityId) {}
    public Instant getUpdatedAt() { return createdAt != null ? createdAt : Instant.now(); }
    public void setUpdatedAt(Instant updatedAt) {}
    public void setEndedAt(Instant endedAt) { this.resolvedAt = endedAt; }
    public Instant getEndedAt() { return resolvedAt; }
}
