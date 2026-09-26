package com.aerosentinel.dto.city;

import java.time.Instant;
import java.util.UUID;

public class CityResponse {
    private UUID id;
    private String name;
    private String state;
    private String country;
    private String timezone;
    private Double latitude;
    private Double longitude;
    private Boolean active;
    private Instant createdAt;

    public CityResponse() {}

    public CityResponse(UUID id, String name, String state, String country, String timezone,
                        Double latitude, Double longitude, Boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.state = state;
        this.country = country;
        this.timezone = timezone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
