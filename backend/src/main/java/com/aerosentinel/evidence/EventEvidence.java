package com.aerosentinel.evidence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_evidence")
public class EventEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "evidence_key", nullable = false, length = 100)
    private String evidenceKey;

    @Column(name = "evidence_value", nullable = false, columnDefinition = "TEXT")
    private String evidenceValue;

    @Column(nullable = false)
    private Double weight = 1.0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public EventEvidence() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getEvidenceKey() { return evidenceKey; }
    public void setEvidenceKey(String evidenceKey) { this.evidenceKey = evidenceKey; }
    public String getEvidenceValue() { return evidenceValue; }
    public void setEvidenceValue(String evidenceValue) { this.evidenceValue = evidenceValue; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
