package com.aerosentinel.dto.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GridCellResponse {
    private String h3Index;
    private UUID cityId;
    private Integer resolution;
    private LatLngPoint center;
    private List<LatLngPoint> boundary = new ArrayList<>();

    public GridCellResponse() {}

    public GridCellResponse(String h3Index, UUID cityId, Integer resolution, LatLngPoint center, List<LatLngPoint> boundary) {
        this.h3Index = h3Index;
        this.cityId = cityId;
        this.resolution = resolution;
        this.center = center;
        this.boundary = boundary != null ? boundary : new ArrayList<>();
    }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public Integer getResolution() { return resolution; }
    public void setResolution(Integer resolution) { this.resolution = resolution; }

    public LatLngPoint getCenter() { return center; }
    public void setCenter(LatLngPoint center) { this.center = center; }

    public List<LatLngPoint> getBoundary() { return boundary; }
    public void setBoundary(List<LatLngPoint> boundary) {
        this.boundary = boundary != null ? boundary : new ArrayList<>();
    }
}
