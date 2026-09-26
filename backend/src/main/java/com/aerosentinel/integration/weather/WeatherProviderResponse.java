package com.aerosentinel.integration.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherProviderResponse {

    private Double latitude;
    private Double longitude;
    private String timezone;

    @JsonProperty("hourly_units")
    private Map<String, String> hourlyUnits;

    private HourlyData hourly;

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Map<String, String> getHourlyUnits() { return hourlyUnits; }
    public void setHourlyUnits(Map<String, String> hourlyUnits) { this.hourlyUnits = hourlyUnits; }
    public HourlyData getHourly() { return hourly; }
    public void setHourly(HourlyData hourly) { this.hourly = hourly; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HourlyData {
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m;

        @JsonProperty("relative_humidity_2m")
        private List<Double> relativeHumidity2m;

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed10m;

        @JsonProperty("wind_direction_10m")
        private List<Double> windDirection10m;

        private List<Double> precipitation;

        @JsonProperty("surface_pressure")
        private List<Double> surfacePressure;

        @JsonProperty("boundary_layer_height")
        private List<Double> boundaryLayerHeight;

        public List<String> getTime() { return time; }
        public void setTime(List<String> time) { this.time = time; }
        public List<Double> getTemperature2m() { return temperature2m; }
        public void setTemperature2m(List<Double> temperature2m) { this.temperature2m = temperature2m; }
        public List<Double> getRelativeHumidity2m() { return relativeHumidity2m; }
        public void setRelativeHumidity2m(List<Double> relativeHumidity2m) { this.relativeHumidity2m = relativeHumidity2m; }
        public List<Double> getWindSpeed10m() { return windSpeed10m; }
        public void setWindSpeed10m(List<Double> windSpeed10m) { this.windSpeed10m = windSpeed10m; }
        public List<Double> getWindDirection10m() { return windDirection10m; }
        public void setWindDirection10m(List<Double> windDirection10m) { this.windDirection10m = windDirection10m; }
        public List<Double> getPrecipitation() { return precipitation; }
        public void setPrecipitation(List<Double> precipitation) { this.precipitation = precipitation; }
        public List<Double> getSurfacePressure() { return surfacePressure; }
        public void setSurfacePressure(List<Double> surfacePressure) { this.surfacePressure = surfacePressure; }
        public List<Double> getBoundaryLayerHeight() { return boundaryLayerHeight; }
        public void setBoundaryLayerHeight(List<Double> boundaryLayerHeight) { this.boundaryLayerHeight = boundaryLayerHeight; }
    }
}
