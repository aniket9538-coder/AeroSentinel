package com.aerosentinel.grid;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grid_cells")
public class GridCell {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "h3_index", unique = true, nullable = false, length = 30)
    private String h3Index;

    @Column(nullable = false)
    private Integer resolution = 8;

    @Column(name = "center_latitude", nullable = false)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = false)
    private Double centerLongitude;

    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public GridCell() {}

    public GridCell(UUID cityId, String h3Index, Double centerLatitude, Double centerLongitude) {
        this.cityId = cityId;
        this.h3Index = h3Index;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public Integer getResolution() { return resolution; }
    public void setResolution(Integer resolution) { this.resolution = resolution; }
    public Double getCenterLatitude() { return centerLatitude; }
    public void setCenterLatitude(Double centerLatitude) { this.centerLatitude = centerLatitude; }
    public Double getCenterLongitude() { return centerLongitude; }
    public void setCenterLongitude(Double centerLongitude) { this.centerLongitude = centerLongitude; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
