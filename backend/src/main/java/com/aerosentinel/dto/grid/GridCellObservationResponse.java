package com.aerosentinel.dto.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GridCellObservationResponse {
    private String h3Index;
    private UUID cityId;
    private List<GridAirObservationResponse> airObservations = new ArrayList<>();
    private List<GridWeatherObservationResponse> weatherObservations = new ArrayList<>();

    public GridCellObservationResponse() {}

    public GridCellObservationResponse(String h3Index, UUID cityId,
                                       List<GridAirObservationResponse> airObservations,
                                       List<GridWeatherObservationResponse> weatherObservations) {
        this.h3Index = h3Index;
        this.cityId = cityId;
        this.airObservations = airObservations != null ? airObservations : new ArrayList<>();
        this.weatherObservations = weatherObservations != null ? weatherObservations : new ArrayList<>();
    }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public List<GridAirObservationResponse> getAirObservations() { return airObservations; }
    public void setAirObservations(List<GridAirObservationResponse> airObservations) {
        this.airObservations = airObservations != null ? airObservations : new ArrayList<>();
    }

    public List<GridWeatherObservationResponse> getWeatherObservations() { return weatherObservations; }
    public void setWeatherObservations(List<GridWeatherObservationResponse> weatherObservations) {
        this.weatherObservations = weatherObservations != null ? weatherObservations : new ArrayList<>();
    }
}
