package com.aerosentinel.dto.air;

import java.util.ArrayList;
import java.util.List;

public class AirQualityHistoryResponse {
    private String stationId;
    private String stationName;
    private List<HistoricalObservationResponse> observations = new ArrayList<>();

    public AirQualityHistoryResponse() {}

    public AirQualityHistoryResponse(String stationId, String stationName, List<HistoricalObservationResponse> observations) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.observations = observations != null ? observations : new ArrayList<>();
    }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public List<HistoricalObservationResponse> getObservations() { return observations; }
    public void setObservations(List<HistoricalObservationResponse> observations) {
        this.observations = observations != null ? observations : new ArrayList<>();
    }
}
