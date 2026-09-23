package com.aerosentinel.grid;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grid_features")
public class GridFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "grid_cell_id", nullable = false)
    private UUID gridCellId;

    @Column(name = "feature_time", nullable = false)
    private Instant featureTime;

    @Column(name = "pm25_current")
    private Double pm25Current;

    @Column(name = "pm25_lag_1h")
    private Double pm25Lag1h;

    @Column(name = "pm25_lag_3h")
    private Double pm25Lag3h;

    @Column(name = "pm25_trend")
    private Double pm25Trend;

    private Double temperature;
    private Double humidity;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_direction")
    private Double windDirection;

    private Double rainfall;

    @Column(name = "fire_count")
    private Integer fireCount = 0;

    @Column(name = "nearest_fire_distance")
    private Double nearestFireDistance;

    @Column(name = "fire_confidence_avg")
    private Double fireConfidenceAvg;

    @Column(name = "satellite_no2")
    private Double satelliteNo2;

    @Column(name = "satellite_so2")
    private Double satelliteSo2;

    @Column(name = "citizen_report_count")
    private Integer citizenReportCount = 0;

    @Column(name = "nearest_station_distance")
    private Double nearestStationDistance;

    @Column(name = "data_completeness")
    private Double dataCompleteness = 1.0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public GridFeature() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGridCellId() { return gridCellId; }
    public void setGridCellId(UUID gridCellId) { this.gridCellId = gridCellId; }
    public Instant getFeatureTime() { return featureTime; }
    public void setFeatureTime(Instant featureTime) { this.featureTime = featureTime; }
    public Double getPm25Current() { return pm25Current; }
    public void setPm25Current(Double pm25Current) { this.pm25Current = pm25Current; }
    public Double getPm25Lag1h() { return pm25Lag1h; }
    public void setPm25Lag1h(Double pm25Lag1h) { this.pm25Lag1h = pm25Lag1h; }
    public Double getPm25Lag3h() { return pm25Lag3h; }
    public void setPm25Lag3h(Double pm25Lag3h) { this.pm25Lag3h = pm25Lag3h; }
    public Double getPm25Trend() { return pm25Trend; }
    public void setPm25Trend(Double pm25Trend) { this.pm25Trend = pm25Trend; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }
    public Double getWindDirection() { return windDirection; }
    public void setWindDirection(Double windDirection) { this.windDirection = windDirection; }
    public Double getRainfall() { return rainfall; }
    public void setRainfall(Double rainfall) { this.rainfall = rainfall; }
    public Integer getFireCount() { return fireCount; }
    public void setFireCount(Integer fireCount) { this.fireCount = fireCount; }
    public Double getNearestFireDistance() { return nearestFireDistance; }
    public void setNearestFireDistance(Double nearestFireDistance) { this.nearestFireDistance = nearestFireDistance; }
    public Double getFireConfidenceAvg() { return fireConfidenceAvg; }
    public void setFireConfidenceAvg(Double fireConfidenceAvg) { this.fireConfidenceAvg = fireConfidenceAvg; }
    public Double getSatelliteNo2() { return satelliteNo2; }
    public void setSatelliteNo2(Double satelliteNo2) { this.satelliteNo2 = satelliteNo2; }
    public Double getSatelliteSo2() { return satelliteSo2; }
    public void setSatelliteSo2(Double satelliteSo2) { this.satelliteSo2 = satelliteSo2; }
    public Integer getCitizenReportCount() { return citizenReportCount; }
    public void setCitizenReportCount(Integer citizenReportCount) { this.citizenReportCount = citizenReportCount; }
    public Double getNearestStationDistance() { return nearestStationDistance; }
    public void setNearestStationDistance(Double nearestStationDistance) { this.nearestStationDistance = nearestStationDistance; }
    public Double getDataCompleteness() { return dataCompleteness; }
    public void setDataCompleteness(Double dataCompleteness) { this.dataCompleteness = dataCompleteness; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
