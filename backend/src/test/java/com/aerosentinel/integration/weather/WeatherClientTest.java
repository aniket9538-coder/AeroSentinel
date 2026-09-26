package com.aerosentinel.integration.weather;

import com.aerosentinel.spatial.H3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private WeatherClient weatherClient;
    private H3Service h3Service;

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplateBuilder().build();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        h3Service = new H3Service(8);
        weatherClient = new WeatherClient(restTemplate, h3Service, BASE_URL);
    }

    @Test
    @DisplayName("1. Coordinate validation rejects invalid latitude before making HTTP request")
    void testInvalidLatitudeRejected() {
        assertThatThrownBy(() -> weatherClient.fetchHourlyWeather(95.0, 73.84))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid latitude");

        mockServer.verify();
    }

    @Test
    @DisplayName("2. Coordinate validation rejects invalid longitude before making HTTP request")
    void testInvalidLongitudeRejected() {
        assertThatThrownBy(() -> weatherClient.fetchHourlyWeather(18.53, 195.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid longitude");

        mockServer.verify();
    }

    @Test
    @DisplayName("3. Successful 200 response parses hourly meteorological variables")
    void testSuccessfulResponse() {
        String jsonPayload = """
                {
                  "latitude": 18.53,
                  "longitude": 73.84,
                  "timezone": "Asia/Kolkata",
                  "hourly": {
                    "time": ["2026-09-25T10:00", "2026-09-25T11:00"],
                    "temperature_2m": [28.2, 29.5],
                    "relative_humidity_2m": [62.0, 58.0],
                    "wind_speed_10m": [11.5, 13.0],
                    "wind_direction_10m": [260.0, 275.0],
                    "precipitation": [0.0, 0.1]
                  }
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonPayload, MediaType.APPLICATION_JSON));

        Optional<WeatherProviderResponse> resultOpt = weatherClient.fetchHourlyWeather(18.5314, 73.8446);

        assertThat(resultOpt).isPresent();
        WeatherProviderResponse response = resultOpt.get();
        assertThat(response.getHourly()).isNotNull();
        assertThat(response.getHourly().getTime()).hasSize(2);
        assertThat(response.getHourly().getTemperature2m()).containsExactly(28.2, 29.5);
        assertThat(response.getHourly().getRelativeHumidity2m()).containsExactly(62.0, 58.0);

        mockServer.verify();
    }

    @Test
    @DisplayName("4. HTTP 500 error from Open-Meteo returns empty Optional without fabricating fake data")
    void testHttp500ReturnsEmpty() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Optional<WeatherProviderResponse> result = weatherClient.fetchHourlyWeather(18.5314, 73.8446);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("5. HTTP 404 error returns empty Optional without fabricating fake data")
    void testHttp404ReturnsEmpty() {
        mockServer.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<WeatherProviderResponse> result = weatherClient.fetchHourlyWeather(18.5314, 73.8446);

        assertThat(result).isEmpty();
        mockServer.verify();
    }
}
