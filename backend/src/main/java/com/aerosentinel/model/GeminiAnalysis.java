package com.aerosentinel.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity mapping the existing PostgreSQL `gemini_analyses` table.
 * Preserves event, spatial, lineage, and structured AI explanation attributes.
 */
@Entity
@Table(name = "gemini_analyses")
public class GeminiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "prediction_id")
    private UUID predictionId;

    @Column(name = "citizen_report_id")
    private UUID citizenReportId;

    @Column(name = "h3_index", length = 30)
    private String h3Index;

    @Column(name = "model_name", length = 50)
    private String modelName;

    @Column(name = "model_version", length = 50)
    private String modelVersion = "gemini-2.0-flash";

    @Column(name = "prompt_version", length = 50)
    private String promptVersion = "structured_event_explanation_v001";

    @Column(name = "detected_category", length = 50)
    private String detectedCategory;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "narrative_summary", columnDefinition = "TEXT")
    private String narrativeSummary;

    @Column(name = "event_summary_public", columnDefinition = "TEXT")
    private String eventSummaryPublic;

    @Column(name = "event_summary_analyst", columnDefinition = "TEXT")
    private String eventSummaryAnalyst;

    @Column(name = "detected_condition", length = 100)
    private String detectedCondition;

    @Column(name = "forecast_trajectory", columnDefinition = "TEXT")
    private String forecastTrajectory;

    @Column(name = "uncertainty_statement", columnDefinition = "TEXT")
    private String uncertaintyStatement;

    @Column(name = "is_grounded")
    private Boolean isGrounded = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public GeminiAnalysis() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(UUID predictionId) {
        this.predictionId = predictionId;
    }

    public String getH3Index() {
        return h3Index;
    }

    public void setH3Index(String h3Index) {
        this.h3Index = h3Index;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getEventSummaryPublic() {
        return eventSummaryPublic;
    }

    public void setEventSummaryPublic(String eventSummaryPublic) {
        this.eventSummaryPublic = eventSummaryPublic;
    }

    public String getEventSummaryAnalyst() {
        return eventSummaryAnalyst;
    }

    public void setEventSummaryAnalyst(String eventSummaryAnalyst) {
        this.eventSummaryAnalyst = eventSummaryAnalyst;
    }

    public String getDetectedCondition() {
        return detectedCondition;
    }

    public void setDetectedCondition(String detectedCondition) {
        this.detectedCondition = detectedCondition;
    }

    public String getForecastTrajectory() {
        return forecastTrajectory;
    }

    public void setForecastTrajectory(String forecastTrajectory) {
        this.forecastTrajectory = forecastTrajectory;
    }

    public String getUncertaintyStatement() {
        return uncertaintyStatement;
    }

    public void setUncertaintyStatement(String uncertaintyStatement) {
        this.uncertaintyStatement = uncertaintyStatement;
    }

    public Boolean getIsGrounded() {
        return isGrounded;
    }

    public void setIsGrounded(Boolean isGrounded) {
        this.isGrounded = isGrounded;
    }

    public UUID getCitizenReportId() {
        return citizenReportId;
    }

    public void setCitizenReportId(UUID citizenReportId) {
        this.citizenReportId = citizenReportId;
    }

    public String getDetectedCategory() {
        return detectedCategory;
    }

    public void setDetectedCategory(String detectedCategory) {
        this.detectedCategory = detectedCategory;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getNarrativeSummary() {
        return narrativeSummary;
    }

    public void setNarrativeSummary(String narrativeSummary) {
        this.narrativeSummary = narrativeSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
