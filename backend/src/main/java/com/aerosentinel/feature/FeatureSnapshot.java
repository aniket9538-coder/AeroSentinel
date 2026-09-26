package com.aerosentinel.feature;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feature_snapshots")
public class FeatureSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "h3_index", nullable = false, length = 30)
    private String h3Index;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "feature_schema_version", nullable = false, length = 50)
    private String featureSchemaVersion = "f3-features-v1";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", nullable = false, columnDefinition = "jsonb")
    private String features;

    @Column(name = "quality_status", nullable = false, length = 30)
    private String qualityStatus = FeatureQualityStatus.VALID.name();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "missing_features", columnDefinition = "TEXT[]")
    private String[] missingFeatures;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public FeatureSnapshot() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
    public String getFeatureSchemaVersion() { return featureSchemaVersion; }
    public void setFeatureSchemaVersion(String featureSchemaVersion) { this.featureSchemaVersion = featureSchemaVersion; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }
    public String[] getMissingFeatures() { return missingFeatures; }
    public void setMissingFeatures(String[] missingFeatures) { this.missingFeatures = missingFeatures; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
