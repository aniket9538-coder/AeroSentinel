package com.aerosentinel.dto.air;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LatestAirQualityResponse {
    private UUID cityId;
    private String cityName;
    private List<AirQualityObservationResponse> observations = new ArrayList<>();

    public LatestAirQualityResponse() {}

    public LatestAirQualityResponse(UUID cityId, String cityName, List<AirQualityObservationResponse> observations) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.observations = observations != null ? observations : new ArrayList<>();
    }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public List<AirQualityObservationResponse> getObservations() { return observations; }
    public void setObservations(List<AirQualityObservationResponse> observations) {
        this.observations = observations != null ? observations : new ArrayList<>();
    }
}
