package com.aerosentinel.integration.weather;

import com.aerosentinel.spatial.H3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Authoritative HTTP client for fetching real meteorological data from Open-Meteo API.
 * Uses official variables (temperature_2m, relative_humidity_2m, wind_speed_10m,
 * wind_direction_10m, precipitation) in Asia/Kolkata timezone with metric units.
 */
@Component
public class WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    public static final String DEFAULT_BASE_URL = "https://api.open-meteo.com/v1/forecast";
    public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final H3Service h3Service;

    @org.springframework.beans.factory.annotation.Autowired
    public WeatherClient(RestTemplateBuilder restTemplateBuilder,
                         H3Service h3Service,
                         @Value("${app.external.weather.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}") String baseUrl,
                         @Value("${app.external.weather.open-meteo.timeout-ms:15000}") long timeoutMs) {
        this.h3Service = h3Service;
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : DEFAULT_BASE_URL;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        log.info("Initialized WeatherClient with baseUrl='{}', timeoutMs={}", this.baseUrl, timeoutMs);
    }

    /**
     * Overloaded constructor for tests allowing injection of custom RestTemplate.
     */
    public WeatherClient(RestTemplate restTemplate, H3Service h3Service, String baseUrl) {
        this.restTemplate = restTemplate;
        this.h3Service = h3Service;
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : DEFAULT_BASE_URL;
    }

    /**
     * Fetches hourly weather telemetry for given coordinates.
     * Requests past 1 day of historical observations and current day forecast.
     */
    public Optional<WeatherProviderResponse> fetchHourlyWeather(double latitude, double longitude) {
        return fetchHourlyWeather(latitude, longitude, 1, 1);
    }

    /**
     * Fetches hourly weather telemetry with custom pastDays and forecastDays.
     */
    public Optional<WeatherProviderResponse> fetchHourlyWeather(double latitude, double longitude, int pastDays, int forecastDays) {
        // Validate coordinates
        h3Service.validateCoordinates(latitude, longitude);

        URI targetUri = UriComponentsBuilder.fromUriString(this.baseUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("hourly", "temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,precipitation,surface_pressure,boundary_layer_height")
                .queryParam("past_days", Math.max(0, pastDays))
                .queryParam("forecast_days", Math.max(1, forecastDays))
                .queryParam("timezone", DEFAULT_TIMEZONE)
                .build()
                .toUri();

        log.debug("Sending Open-Meteo HTTP request to {}", targetUri);

        try {
            ResponseEntity<WeatherProviderResponse> response = restTemplate.getForEntity(targetUri, WeatherProviderResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Open-Meteo response received successfully for coordinates ({}, {})", latitude, longitude);
                return Optional.of(response.getBody());
            } else {
                log.warn("Open-Meteo returned unexpected status {} for coordinates ({}, {})",
                        response.getStatusCode(), latitude, longitude);
                return Optional.empty();
            }
        } catch (HttpStatusCodeException e) {
            log.error("Open-Meteo HTTP error status={} body='{}' for coordinates ({}, {})",
                    e.getStatusCode(), e.getResponseBodyAsString(), latitude, longitude);
            return Optional.empty();
        } catch (ResourceAccessException e) {
            log.error("Open-Meteo network or timeout failure for coordinates ({}, {}): {}",
                    latitude, longitude, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during Open-Meteo fetch for coordinates ({}, {}): {}",
                    latitude, longitude, e.getMessage(), e);
            return Optional.empty();
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
