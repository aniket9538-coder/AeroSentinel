package com.aerosentinel.inspection;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_verifications")
public class FieldVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "h3_index", nullable = false, length = 30)
    private String h3Index;

    @Column(name = "prediction_id")
    private UUID predictionId;

    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus;

    @Column(name = "verification_result", length = 50)
    private String verificationResult;

    @Column(name = "observed_conditions", nullable = false, columnDefinition = "TEXT")
    private String observedConditions;

    @Column(name = "inspector_notes", columnDefinition = "TEXT")
    private String inspectorNotes;

    @Column(name = "evidence_references", columnDefinition = "TEXT")
    private String evidenceReferences;

    @Column(name = "verified_by", length = 150)
    private String verifiedBy;

    @Column(name = "inspected_at")
    private Instant inspectedAt = Instant.now();

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public FieldVerification() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInspectionId() { return inspectionId; }
    public void setInspectionId(UUID inspectionId) { this.inspectionId = inspectionId; }

    public UUID getAlertId() { return alertId; }
    public void setAlertId(UUID alertId) { this.alertId = alertId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }

    public UUID getPredictionId() { return predictionId; }
    public void setPredictionId(UUID predictionId) { this.predictionId = predictionId; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationResult() { return verificationResult; }
    public void setVerificationResult(String verificationResult) { this.verificationResult = verificationResult; }

    public String getObservedConditions() { return observedConditions; }
    public void setObservedConditions(String observedConditions) { this.observedConditions = observedConditions; }

    public String getInspectorNotes() { return inspectorNotes; }
    public void setInspectorNotes(String inspectorNotes) { this.inspectorNotes = inspectorNotes; }

    public String getEvidenceReferences() { return evidenceReferences; }
    public void setEvidenceReferences(String evidenceReferences) { this.evidenceReferences = evidenceReferences; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public Instant getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(Instant inspectedAt) { this.inspectedAt = inspectedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
