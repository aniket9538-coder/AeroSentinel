package com.aerosentinel.dto.weather;

import java.time.Instant;
import java.util.UUID;

public class WeatherLatestResponse {
    private UUID cityId;
    private String cityName;
    private Double temperature;
    private Double humidity;
    private Double windSpeed;
    private Double windDirection;
    private Double rainfall;
    private Instant observedAt;
    private String source;
    private String h3Index;

    public WeatherLatestResponse() {}

    public WeatherLatestResponse(UUID cityId, String cityName) {
        this.cityId = cityId;
        this.cityName = cityName;
    }

    public WeatherLatestResponse(UUID cityId, String cityName, Double temperature, Double humidity,
                                 Double windSpeed, Double windDirection, Double rainfall,
                                 Instant observedAt, String source, String h3Index) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.rainfall = rainfall;
        this.observedAt = observedAt;
        this.source = source;
        this.h3Index = h3Index;
    }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

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

    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant observedAt) { this.observedAt = observedAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getH3Index() { return h3Index; }
    public void setH3Index(String h3Index) { this.h3Index = h3Index; }
}
