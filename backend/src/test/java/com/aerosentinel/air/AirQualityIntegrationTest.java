package com.aerosentinel.air;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AirQualityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";

    @Test
    @DisplayName("Integration Test 1: GET /api/v1/cities -> HTTP 200, 3 seeded cities available")
    void testGetCities() throws Exception {
        mockMvc.perform(get("/api/v1/cities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Pune", "Mumbai", "Delhi")));
    }

    @Test
    @DisplayName("Integration Test 2: GET /api/v1/cities/{PUNE_ID}/air-quality/latest -> HTTP 200, 3 stations with DB PM2.5 values")
    void testGetLatestAirQualityForPune() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/air-quality/latest")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.cityName", is("Pune")))
                .andExpect(jsonPath("$.observations", hasSize(3)))
                .andExpect(jsonPath("$.observations[*].stationId", containsInAnyOrder("PUN-001", "PUN-002", "PUN-003")))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-001')].pm25", contains(78.0)))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-002')].pm25", contains(62.0)))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-003')].pm25", contains(86.0)))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-001')].source", contains("CPCB")))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-001')].quality", contains("VALID")));
    }

    @Test
    @DisplayName("Integration Test 3: GET /api/v1/stations/PUN-001/air-quality (full day) -> HTTP 200, 12 observations")
    void testGetStationHistoryPun001() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                .param("from", "2026-09-24T00:00:00Z")
                .param("to", "2026-09-24T23:59:59Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-001")))
                .andExpect(jsonPath("$.stationName", is("Shivajinagar CAAQMS")))
                .andExpect(jsonPath("$.observations", hasSize(12)))
                .andExpect(jsonPath("$.observations[0].observedAt", is("2026-09-24T00:00:00Z")))
                .andExpect(jsonPath("$.observations[0].pm25", is(72.0)))
                .andExpect(jsonPath("$.observations[0].source", is("CPCB")))
                .andExpect(jsonPath("$.observations[0].quality", is("VALID")))
                .andExpect(jsonPath("$.observations[11].observedAt", is("2026-09-24T22:00:00Z")))
                .andExpect(jsonPath("$.observations[11].pm25", is(78.0)));
    }

    @Test
    @DisplayName("Integration Test 4: PUN-002 full-day history -> HTTP 200, 12 observations")
    void testGetStationHistoryPun002() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-002/air-quality")
                .param("from", "2026-09-24T00:00:00Z")
                .param("to", "2026-09-24T23:59:59Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-002")))
                .andExpect(jsonPath("$.stationName", is("Katraj Air Station")))
                .andExpect(jsonPath("$.observations", hasSize(12)))
                .andExpect(jsonPath("$.observations[0].pm25", is(58.0)))
                .andExpect(jsonPath("$.observations[0].source", is("MPCB")))
                .andExpect(jsonPath("$.observations[11].pm25", is(62.0)));
    }

    @Test
    @DisplayName("Integration Test 5: PUN-003 full-day history -> HTTP 200, 12 observations")
    void testGetStationHistoryPun003() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-003/air-quality")
                .param("from", "2026-09-24T00:00:00Z")
                .param("to", "2026-09-24T23:59:59Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-003")))
                .andExpect(jsonPath("$.stationName", is("Hadapsar Industrial Zone")))
                .andExpect(jsonPath("$.observations", hasSize(12)))
                .andExpect(jsonPath("$.observations[0].pm25", is(76.0)))
                .andExpect(jsonPath("$.observations[0].source", is("MPCB")))
                .andExpect(jsonPath("$.observations[11].pm25", is(86.0)));
    }

    @Test
    @DisplayName("Integration Test 6: Invalid station -> HTTP 404")
    void testGetInvalidStationReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/stations/INVALID-CODE/air-quality")
                .param("from", "2026-09-24T00:00:00Z")
                .param("to", "2026-09-24T23:59:59Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("Monitoring station not found")));
    }

    @Test
    @DisplayName("Integration Test 7: Invalid date range (from > to) -> HTTP 400")
    void testInvalidDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                .param("from", "2026-09-25T00:00:00Z")
                .param("to", "2026-09-24T00:00:00Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("must not be after 'to'")));
    }

    @Test
    @DisplayName("Integration Test 8: No-data range -> Controlled empty response")
    void testNoDataRangeReturnsControlledEmptyResponse() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                .param("from", "2020-01-01T00:00:00Z")
                .param("to", "2020-01-02T00:00:00Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-001")))
                .andExpect(jsonPath("$.stationName", is("Shivajinagar CAAQMS")))
                .andExpect(jsonPath("$.observations", hasSize(0)));
    }

    @Test
    @DisplayName("Integration Test 9: Malformed date -> HTTP 400")
    void testMalformedDateReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                .param("from", "not-a-valid-date")
                .param("to", "2026-09-24T23:59:59Z")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", containsString("Invalid 'from' timestamp format")));
    }

    @Test
    @DisplayName("Integration Test 10: Missing required date parameters -> HTTP 400")
    void testMissingDateParamsReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("Integration Test 11: Invalid city ID -> HTTP 404")
    void testInvalidCityReturns404() throws Exception {
        String randomCityId = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get("/api/v1/cities/" + randomCityId + "/air-quality/latest")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }
}
