package com.aerosentinel.dto.grid;

import java.time.Instant;
import java.util.UUID;

public class GridAirObservationResponse {
    private UUID id;
    private String stationId;
    private String stationName;
    private Double pm25;
    private Instant observedAt;
    private String source;
    private String quality;
    private String h3Index;

    public GridAirObservationResponse() {}

    public GridAirObservationResponse(UUID id, String stationId, String stationName, Double pm25,
                                      Instant observedAt, String source, String quality, String h3Index) {
        this.id = id;
        this.stationId = stationId;
        this.stationName = stationName;
        this.pm25 = pm25;
        this.observedAt = observedAt;
        this.source = source;
        this.quality = quality;
        this.h3Index = h3Index;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
}
