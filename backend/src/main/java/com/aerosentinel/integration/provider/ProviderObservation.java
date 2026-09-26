package com.aerosentinel.integration.provider;

import java.time.Instant;

public class ProviderObservation {

    private String providerStationId;
    private Double latitude;
    private Double longitude;
    private Instant observedAt;
    private Double pm25;
    private String source;
    private String quality = "VALID";
    private String rawParameter;
    private String unit;

    public ProviderObservation() {}

    public ProviderObservation(String providerStationId, Double latitude, Double longitude,
                               Instant observedAt, Double pm25, String source, String quality) {
        this.providerStationId = providerStationId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.observedAt = observedAt;
        this.pm25 = pm25;
        this.source = source;
        this.quality = quality;
    }

    public String getProviderStationId() { return providerStationId; }
    public void setProviderStationId(String providerStationId) { this.providerStationId = providerStationId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getRawParameter() { return rawParameter; }
    public void setRawParameter(String rawParameter) { this.rawParameter = rawParameter; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() {
        return "ProviderObservation{" +
                "providerStationId='" + providerStationId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", observedAt=" + observedAt +
                ", pm25=" + pm25 +
                ", source='" + source + '\'' +
                ", quality='" + quality + '\'' +
                '}';
    }
}
