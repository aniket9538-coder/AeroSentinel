package com.aerosentinel.integration.weather;

import com.aerosentinel.weather.WeatherObservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherMapperTest {

    private WeatherMapper weatherMapper;
    private static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final double PUNE_LAT = 18.5314;
    private static final double PUNE_LNG = 73.8446;

    @BeforeEach
    void setUp() {
        weatherMapper = new WeatherMapper();
    }

    private WeatherProviderResponse createSampleResponse() {
        WeatherProviderResponse response = new WeatherProviderResponse();
        response.setLatitude(PUNE_LAT);
        response.setLongitude(PUNE_LNG);
        response.setTimezone("Asia/Kolkata");

        WeatherProviderResponse.HourlyData hourly = new WeatherProviderResponse.HourlyData();
        hourly.setTime(Arrays.asList("2026-09-25T10:00", "2026-09-25T11:00", "2026-09-25T12:00"));
        hourly.setTemperature2m(Arrays.asList(28.5, 29.2, 30.1));
        hourly.setRelativeHumidity2m(Arrays.asList(65.0, 60.0, 55.0));
        hourly.setWindSpeed10m(Arrays.asList(12.5, 14.0, 15.2));
        hourly.setWindDirection10m(Arrays.asList(270.0, 280.0, 285.0));
        hourly.setPrecipitation(Arrays.asList(0.0, 0.2, 0.0));
        hourly.setSurfacePressure(Arrays.asList(946.3, 945.8, 945.4));

        response.setHourly(hourly);
        return response;
    }

    @Test
    @DisplayName("1. Valid provider response maps correctly to domain WeatherObservation entities")
    void testValidMapping() {
        WeatherProviderResponse response = createSampleResponse();
        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);

        assertThat(observations).hasSize(3);

        WeatherObservation first = observations.get(0);
        assertThat(first.getCityId()).isEqualTo(PUNE_CITY_ID);
        assertThat(first.getLatitude()).isEqualTo(PUNE_LAT);
        assertThat(first.getLongitude()).isEqualTo(PUNE_LNG);
        assertThat(first.getTemperature()).isEqualTo(28.5);
        assertThat(first.getHumidity()).isEqualTo(65.0);
        assertThat(first.getWindSpeed()).isEqualTo(12.5);
        assertThat(first.getWindDirection()).isEqualTo(270.0);
        assertThat(first.getRainfall()).isEqualTo(0.0);
        assertThat(first.getPressure()).isEqualTo(946.3);
        assertThat(first.getSource()).isEqualTo("OPEN_METEO");
        assertThat(first.getCreatedAt()).isNotNull();

        // 2026-09-25T10:00 in Asia/Kolkata (+05:30) is 2026-09-25T04:30:00Z in UTC
        assertThat(first.getObservedAt()).isEqualTo(Instant.parse("2026-09-25T04:30:00Z"));
    }

    @Test
    @DisplayName("2. Timestamp parsing correctly converts Asia/Kolkata to UTC Instant")
    void testTimestampParsing() {
        WeatherProviderResponse response = createSampleResponse();
        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);

        // 11:00 IST -> 05:30 UTC
        assertThat(observations.get(1).getObservedAt()).isEqualTo(Instant.parse("2026-09-25T05:30:00Z"));
        // 12:00 IST -> 06:30 UTC
        assertThat(observations.get(2).getObservedAt()).isEqualTo(Instant.parse("2026-09-25T06:30:00Z"));
    }

    @Test
    @DisplayName("3. Source provenance is strictly OPEN_METEO")
    void testSourceProvenance() {
        WeatherProviderResponse response = createSampleResponse();
        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);

        for (WeatherObservation obs : observations) {
            assertThat(obs.getSource()).isEqualTo("OPEN_METEO");
        }
    }

    @Test
    @DisplayName("4. Meteorological validation: invalid humidity (< 0 or > 100) is rejected")
    void testInvalidHumidityRejected() {
        WeatherProviderResponse response = createSampleResponse();
        List<Double> badHumidities = new ArrayList<>(Arrays.asList(65.0, 150.0, -10.0));
        response.getHourly().setRelativeHumidity2m(badHumidities);

        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);
        // Only the first point with humidity 65.0 should be accepted
        assertThat(observations).hasSize(1);
        assertThat(observations.get(0).getHumidity()).isEqualTo(65.0);
    }

    @Test
    @DisplayName("5. Meteorological validation: negative wind speed is rejected")
    void testNegativeWindSpeedRejected() {
        WeatherProviderResponse response = createSampleResponse();
        List<Double> badWind = new ArrayList<>(Arrays.asList(12.5, -5.0, 15.2));
        response.getHourly().setWindSpeed10m(badWind);

        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);
        assertThat(observations).hasSize(2);
        assertThat(observations.stream().map(WeatherObservation::getWindSpeed)).containsExactly(12.5, 15.2);
    }

    @Test
    @DisplayName("6. Meteorological validation: wind direction outside [0, 360] is rejected")
    void testInvalidWindDirectionRejected() {
        WeatherProviderResponse response = createSampleResponse();
        List<Double> badDirs = new ArrayList<>(Arrays.asList(270.0, 400.0, -1.0));
        response.getHourly().setWindDirection10m(badDirs);

        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);
        assertThat(observations).hasSize(1);
        assertThat(observations.get(0).getWindDirection()).isEqualTo(270.0);
    }

    @Test
    @DisplayName("7. Meteorological validation: negative rainfall is rejected")
    void testNegativeRainfallRejected() {
        WeatherProviderResponse response = createSampleResponse();
        List<Double> badRain = new ArrayList<>(Arrays.asList(0.0, -1.0, 0.0));
        response.getHourly().setPrecipitation(badRain);

        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);
        assertThat(observations).hasSize(2);
    }

    @Test
    @DisplayName("8. Non-finite or missing temperature points are rejected")
    void testNonFiniteTemperatureRejected() {
        WeatherProviderResponse response = createSampleResponse();
        List<Double> badTemps = new ArrayList<>(Arrays.asList(Double.NaN, 29.2, Double.POSITIVE_INFINITY));
        response.getHourly().setTemperature2m(badTemps);

        List<WeatherObservation> observations = weatherMapper.mapToWeatherObservations(response, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG);
        assertThat(observations).hasSize(1);
        assertThat(observations.get(0).getTemperature()).isEqualTo(29.2);
    }

    @Test
    @DisplayName("9. Null response or empty hourly returns empty list without exception")
    void testNullOrEmptyHandling() {
        assertThat(weatherMapper.mapToWeatherObservations(null, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG)).isEmpty();

        WeatherProviderResponse emptyResp = new WeatherProviderResponse();
        assertThat(weatherMapper.mapToWeatherObservations(emptyResp, PUNE_CITY_ID, PUNE_LAT, PUNE_LNG)).isEmpty();
    }
}
