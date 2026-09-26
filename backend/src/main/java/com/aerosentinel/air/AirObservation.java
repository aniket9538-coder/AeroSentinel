package com.aerosentinel.air;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "air_observations")
public class AirObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "city_id", nullable = false)
    private UUID cityId;

    @Column(name = "station_id", nullable = false, length = 50)
    private String stationId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    private Double pm25;
    private Double pm10;
    private Double no2;
    private Double so2;
    private Double co;
    private Double o3;
    private Double aqi;

    @Column(length = 50)
    private String source = "CPCB";

    @Column(name = "data_quality", length = 50)
    private String dataQuality = "VALID";

    @Column(name = "h3_index", length = 30)
    private String h3Index;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public AirObservation() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }
    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }
    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }
    public Double getPm10() { return pm10; }
    public void setPm10(Double pm10) { this.pm10 = pm10; }
    public Double getNo2() { return no2; }
    public void setNo2(Double no2) { this.no2 = no2; }
    public Double getSo2() { return so2; }
    public void setSo2(Double so2) { this.so2 = so2; }
    public Double getCo() { return co; }
    public void setCo(Double co) { this.co = co; }
    public Double getO3() { return o3; }
    public void setO3(Double o3) { this.o3 = o3; }
    public Double getAqi() { return aqi; }
    public void setAqi(Double aqi) { this.aqi = aqi; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }
    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
