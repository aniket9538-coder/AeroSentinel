package com.aerosentinel.integration.weather;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("live")
class LiveOpenMeteoVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(LiveOpenMeteoVerificationTest.class);

    @Autowired
    private WeatherClient weatherClient;

    // Pune Shivajinagar coordinates
    private static final double PUNE_LAT = 18.5314;
    private static final double PUNE_LNG = 73.8446;

    @Test
    @DisplayName("LIVE VERIFICATION: Real Open-Meteo HTTP request returns authentic forecast & history telemetry")
    void testLiveOpenMeteoCall() {
        log.info("Executing live Open-Meteo API verification test for coordinates ({}, {})...", PUNE_LAT, PUNE_LNG);

        Optional<WeatherProviderResponse> responseOpt = weatherClient.fetchHourlyWeather(PUNE_LAT, PUNE_LNG, 1, 1);

        // If host environment has no outbound internet, gracefully skip without generating fake data
        Assumptions.assumeTrue(responseOpt.isPresent(),
                "Open-Meteo endpoint unreachable from this environment; skipping live verification");

        WeatherProviderResponse response = responseOpt.get();
        log.info("Live Open-Meteo response received: timezone={}, elevation={}m",
                response.getTimezone(), response.getHourly() != null ? response.getHourly().getTime().size() : 0);

        assertThat(response.getHourly()).isNotNull();
        assertThat(response.getHourly().getTime()).isNotEmpty();
        assertThat(response.getHourly().getTemperature2m()).isNotEmpty();
        assertThat(response.getHourly().getRelativeHumidity2m()).isNotEmpty();
        assertThat(response.getHourly().getWindSpeed10m()).isNotEmpty();
        assertThat(response.getHourly().getWindDirection10m()).isNotEmpty();
        assertThat(response.getHourly().getPrecipitation()).isNotEmpty();

        // Verify authentic meteorological ranges
        Double sampleTemp = response.getHourly().getTemperature2m().get(0);
        assertThat(sampleTemp).isNotNull().isBetween(-20.0, 60.0);

        Double sampleHumidity = response.getHourly().getRelativeHumidity2m().get(0);
        assertThat(sampleHumidity).isNotNull().isBetween(0.0, 100.0);

        log.info("Live Open-Meteo verification PASSED: sample reading -> temp={}°C, humidity={}%",
                sampleTemp, sampleHumidity);
    }
}
