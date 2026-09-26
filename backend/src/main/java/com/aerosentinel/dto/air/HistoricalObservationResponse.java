package com.aerosentinel.dto.air;

import java.time.Instant;

public class HistoricalObservationResponse {
    private Double pm25;
    private Instant observedAt;
    private String source;
    private String quality;

    public HistoricalObservationResponse() {}

    public HistoricalObservationResponse(Double pm25, Instant observedAt, String source, String quality) {
        this.pm25 = pm25;
        this.observedAt = observedAt;
        this.source = source;
        this.quality = quality;
    }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
}
