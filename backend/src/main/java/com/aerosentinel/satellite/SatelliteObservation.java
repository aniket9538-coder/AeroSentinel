package com.aerosentinel.satellite;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "satellite_observations")
public class SatelliteObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "h3_index", nullable = false, length = 30)
    private String h3Index;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "no2_value")
    private Double no2Value;

    @Column(name = "so2_value")
    private Double so2Value;

    @Column(name = "aerosol_indicator")
    private Double aerosolIndicator;

    @Column(name = "source_product", length = 100)
    private String sourceProduct = "Sentinel-5P";

    @Column(name = "quality_flag", length = 50)
    private String qualityFlag = "QA_PASS";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public SatelliteObservation() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
    public Double getNo2Value() { return no2Value; }
    public void setNo2Value(Double no2Value) { this.no2Value = no2Value; }
    public Double getSo2Value() { return so2Value; }
    public void setSo2Value(Double so2Value) { this.so2Value = so2Value; }
    public Double getAerosolIndicator() { return aerosolIndicator; }
    public void setAerosolIndicator(Double aerosolIndicator) { this.aerosolIndicator = aerosolIndicator; }
    public String getSourceProduct() { return sourceProduct; }
    public void setSourceProduct(String sourceProduct) { this.sourceProduct = sourceProduct; }
    public String getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(String qualityFlag) { this.qualityFlag = qualityFlag; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
