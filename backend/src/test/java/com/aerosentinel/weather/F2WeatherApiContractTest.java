package com.aerosentinel.weather;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 Phase 5 Contract Test: Canonical Latest Weather REST API.
 * Endpoint: GET /api/v1/cities/{cityId}/weather/latest
 *
 * Validates real PostgreSQL persistence, DB-to-API correctness,
 * input validation (400 on malformed UUID, 404 on unknown city),
 * and controlled 200 no-data behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
class F2WeatherApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private CityRepository cityRepository;

    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String MUMBAI_ID = "550e8400-e29b-41d4-a716-446655440002";
    private static final String DELHI_ID = "550e8400-e29b-41d4-a716-446655440003";
    private static final String UNKNOWN_CITY_ID = "00000000-0000-0000-0000-000000000000";

    @Test
    @DisplayName("WEATHER TEST 1: GET /api/v1/cities/{puneId}/weather/latest returns HTTP 200 with real Pune weather")
    void testValidPuneLatestWeather() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/weather/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.cityName", is("Pune")))
                .andExpect(jsonPath("$.temperature", notNullValue()))
                .andExpect(jsonPath("$.humidity", notNullValue()))
                .andExpect(jsonPath("$.windSpeed", notNullValue()))
                .andExpect(jsonPath("$.windDirection", notNullValue()))
                .andExpect(jsonPath("$.rainfall", notNullValue()))
                .andExpect(jsonPath("$.observedAt", notNullValue()))
                .andExpect(jsonPath("$.source", is("OPEN_METEO")))
                .andExpect(jsonPath("$.h3Index", startsWith("88")));
    }

    @Test
    @DisplayName("WEATHER TEST 2: GET /api/v1/cities/{mumbaiId}/weather/latest returns HTTP 200 with real Mumbai weather")
    void testValidMumbaiLatestWeather() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + MUMBAI_ID + "/weather/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(MUMBAI_ID)))
                .andExpect(jsonPath("$.cityName", is("Mumbai")))
                .andExpect(jsonPath("$.temperature", notNullValue()))
                .andExpect(jsonPath("$.humidity", notNullValue()))
                .andExpect(jsonPath("$.windSpeed", notNullValue()))
                .andExpect(jsonPath("$.source", is("OPEN_METEO")))
                .andExpect(jsonPath("$.h3Index", startsWith("88")));
    }

    @Test
    @DisplayName("WEATHER TEST 3: GET /api/v1/cities/{delhiId}/weather/latest returns HTTP 200 with real Delhi weather")
    void testValidDelhiLatestWeather() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + DELHI_ID + "/weather/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(DELHI_ID)))
                .andExpect(jsonPath("$.cityName", is("Delhi")))
                .andExpect(jsonPath("$.temperature", notNullValue()))
                .andExpect(jsonPath("$.humidity", notNullValue()))
                .andExpect(jsonPath("$.windSpeed", notNullValue()))
                .andExpect(jsonPath("$.source", is("OPEN_METEO")))
                .andExpect(jsonPath("$.h3Index", startsWith("88")));
    }

    @Test
    @DisplayName("WEATHER TEST 4: Latest record correctness - API response matches newest DB record by observed_at")
    void testLatestRecordCorrectnessMatchesDb() throws Exception {
        UUID cityUuid = UUID.fromString(PUNE_ID);
        Optional<WeatherObservation> newestDbOpt = weatherRepository.findFirstByCityIdOrderByObservedAtDesc(cityUuid);
        assertThat(newestDbOpt).isPresent();
        WeatherObservation newestDb = newestDbOpt.get();

        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/weather/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.temperature", is(newestDb.getTemperature())))
                .andExpect(jsonPath("$.humidity", is(newestDb.getHumidity())))
                .andExpect(jsonPath("$.windSpeed", is(newestDb.getWindSpeed())))
                .andExpect(jsonPath("$.windDirection", is(newestDb.getWindDirection())))
                .andExpect(jsonPath("$.rainfall", is(newestDb.getRainfall())))
                .andExpect(jsonPath("$.observedAt", is(newestDb.getObservedAt().toString())))
                .andExpect(jsonPath("$.source", is(newestDb.getSource())))
                .andExpect(jsonPath("$.h3Index", is(newestDb.getH3Index())));
    }

    @Test
    @DisplayName("WEATHER TEST 5: Malformed city UUID returns HTTP 400 BAD_REQUEST")
    void testMalformedCityUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/cities/not-a-valid-uuid/weather/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("WEATHER TEST 6: Unknown city UUID returns HTTP 404 NOT_FOUND")
    void testUnknownCityUuidReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + UNKNOWN_CITY_ID + "/weather/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("WEATHER TEST 7: Valid city with no weather observations returns HTTP 200 controlled empty response")
    void testNoDataBehaviorReturns200WithEmptyResponse() throws Exception {
        // Create a temporary city without any weather observations
        City emptyCity = new City();
        emptyCity.setName("TestEmptyCity");
        emptyCity.setState("State");
        emptyCity.setCountry("India");
        emptyCity.setTimezone("Asia/Kolkata");
        emptyCity.setLatitude(12.34);
        emptyCity.setLongitude(56.78);
        emptyCity.setActive(true);
        emptyCity = cityRepository.save(emptyCity);

        try {
            mockMvc.perform(get("/api/v1/cities/" + emptyCity.getId() + "/weather/latest")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cityId", is(emptyCity.getId().toString())))
                    .andExpect(jsonPath("$.cityName", is("TestEmptyCity")))
                    .andExpect(jsonPath("$.temperature").doesNotExist())
                    .andExpect(jsonPath("$.observedAt").doesNotExist())
                    .andExpect(jsonPath("$.source").doesNotExist());
        } finally {
            cityRepository.delete(emptyCity);
        }
    }
}
