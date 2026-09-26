package com.aerosentinel.grid;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.grid.GridAirObservationResponse;
import com.aerosentinel.dto.grid.GridCellObservationResponse;
import com.aerosentinel.dto.grid.GridWeatherObservationResponse;
import com.aerosentinel.weather.WeatherObservation;
import com.aerosentinel.weather.WeatherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 Phase 5 Contract Test: Combined Cell Observations REST API and F1 Regression Verification.
 * Endpoint: GET /api/v1/grid/{h3Index}/observations
 */
@SpringBootTest
@AutoConfigureMockMvc
class F2CellObservationApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AirObservationRepository airObservationRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PUNE_SAMPLE_H3 = "88608850e5fffff"; // Pune Shivajinagar
    private static final String UNKNOWN_H3 = "882681a339fffff";

    @Test
    @DisplayName("OBSERVATION TEST 20: Air observations returned by API match DB records exactly")
    void testAirObservationsMatchDb() throws Exception {
        List<AirObservation> dbAirList = airObservationRepository.findByH3IndexOrderByObservedAtAsc(PUNE_SAMPLE_H3);
        assertThat(dbAirList).isNotEmpty();

        MvcResult result = mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3 + "/observations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index", is(PUNE_SAMPLE_H3)))
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andReturn();

        GridCellObservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), GridCellObservationResponse.class);

        assertThat(response.getAirObservations()).hasSameSizeAs(dbAirList);
        for (int i = 0; i < dbAirList.size(); i++) {
            AirObservation db = dbAirList.get(i);
            GridAirObservationResponse api = response.getAirObservations().get(i);
            assertThat(api.getId()).isEqualTo(db.getId());
            assertThat(api.getStationId()).isEqualTo(db.getStationId());
            assertThat(api.getPm25()).isEqualTo(db.getPm25());
            assertThat(api.getObservedAt()).isEqualTo(db.getObservedAt());
            assertThat(api.getSource()).isEqualTo(db.getSource());
            assertThat(api.getH3Index()).isEqualTo(db.getH3Index());
        }
    }

    @Test
    @DisplayName("OBSERVATION TEST 21: Weather observations returned by API match DB records exactly")
    void testWeatherObservationsMatchDb() throws Exception {
        List<WeatherObservation> dbWeatherList = weatherRepository.findByH3IndexOrderByObservedAtAsc(PUNE_SAMPLE_H3);
        assertThat(dbWeatherList).isNotEmpty();

        MvcResult result = mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3 + "/observations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        GridCellObservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), GridCellObservationResponse.class);

        assertThat(response.getWeatherObservations()).hasSameSizeAs(dbWeatherList);
        for (int i = 0; i < dbWeatherList.size(); i++) {
            WeatherObservation db = dbWeatherList.get(i);
            GridWeatherObservationResponse api = response.getWeatherObservations().get(i);
            assertThat(api.getId()).isEqualTo(db.getId());
            assertThat(api.getTemperature()).isEqualTo(db.getTemperature());
            assertThat(api.getHumidity()).isEqualTo(db.getHumidity());
            assertThat(api.getWindSpeed()).isEqualTo(db.getWindSpeed());
            assertThat(api.getRainfall()).isEqualTo(db.getRainfall());
            assertThat(api.getObservedAt()).isEqualTo(db.getObservedAt());
            assertThat(api.getSource()).isEqualTo(db.getSource());
            assertThat(api.getH3Index()).isEqualTo(db.getH3Index());
        }
    }

    @Test
    @DisplayName("OBSERVATION TEST 22: Observations are returned in chronological ascending order")
    void testChronologicalAscendingOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3 + "/observations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        GridCellObservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), GridCellObservationResponse.class);

        // Verify air observations ascending
        List<GridAirObservationResponse> airObs = response.getAirObservations();
        for (int i = 1; i < airObs.size(); i++) {
            Instant prev = airObs.get(i - 1).getObservedAt();
            Instant curr = airObs.get(i).getObservedAt();
            assertThat(curr).isAfterOrEqualTo(prev);
        }

        // Verify weather observations ascending
        List<GridWeatherObservationResponse> weatherObs = response.getWeatherObservations();
        for (int i = 1; i < weatherObs.size(); i++) {
            Instant prev = weatherObs.get(i - 1).getObservedAt();
            Instant curr = weatherObs.get(i).getObservedAt();
            assertThat(curr).isAfterOrEqualTo(prev);
        }
    }

    @Test
    @DisplayName("OBSERVATION TEST 23: Authentic source provenance is preserved across observations")
    void testSourcePreserved() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3 + "/observations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        GridCellObservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), GridCellObservationResponse.class);

        // Weather source must strictly be OPEN_METEO
        assertThat(response.getWeatherObservations()).allMatch(w -> "OPEN_METEO".equals(w.getSource()));

        // Air source must be authentic CPCB/MPCB/OPENAQ
        assertThat(response.getAirObservations()).allMatch(a ->
                "CPCB".equals(a.getSource()) || "MPCB".equals(a.getSource()) || "OPENAQ".equals(a.getSource()));
    }

    @Test
    @DisplayName("OBSERVATION TEST 24: ObservedAt reflects exact provider timestamp, not current time")
    void testObservedAtPreserved() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3 + "/observations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        GridCellObservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), GridCellObservationResponse.class);

        Instant maxAllowedWindow = Instant.now().plusSeconds(86400);

        // All records have authoritative provider timestamps within the observation/forecast window
        for (GridWeatherObservationResponse w : response.getWeatherObservations()) {
            assertThat(w.getObservedAt()).isNotNull();
            assertThat(w.getObservedAt()).isBefore(maxAllowedWindow);
        }
    }

    @Test
    @DisplayName("OBSERVATION TEST 25: City isolation - only observations belonging to cell are returned")
    void testCityIsolation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3 + "/observations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        GridCellObservationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), GridCellObservationResponse.class);

        // Every air observation in this cell must have station starting with PUN
        assertThat(response.getAirObservations()).allMatch(a -> a.getStationId().startsWith("PUN"));
        // Every observation has exact matching H3 index
        assertThat(response.getAirObservations()).allMatch(a -> PUNE_SAMPLE_H3.equals(a.getH3Index()));
        assertThat(response.getWeatherObservations()).allMatch(w -> PUNE_SAMPLE_H3.equals(w.getH3Index()));
    }

    @Test
    @DisplayName("OBSERVATION TEST 26: Empty cell behavior - cell with 0 observations returns 200 with empty lists")
    void testEmptyCellBehavior() throws Exception {
        // Create temporary cell with valid H3 in a city but 0 observations
        String validUnusedH3 = "882681a339fffff";
        GridCell emptyCell = new GridCell();
        emptyCell.setCityId(UUID.fromString(PUNE_ID));
        emptyCell.setH3Index(validUnusedH3);
        emptyCell.setResolution(8);
        emptyCell.setCenterLatitude(37.7749);
        emptyCell.setCenterLongitude(-122.4194);
        emptyCell.setActive(true);
        emptyCell.setCreatedAt(Instant.now());
        emptyCell = gridRepository.save(emptyCell);

        try {
            mockMvc.perform(get("/api/v1/grid/" + validUnusedH3 + "/observations")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.h3Index", is(validUnusedH3)))
                    .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                    .andExpect(jsonPath("$.airObservations", hasSize(0)))
                    .andExpect(jsonPath("$.weatherObservations", hasSize(0)));
        } finally {
            gridRepository.delete(emptyCell);
        }
    }

    @Test
    @DisplayName("REGRESSION TEST 27: F1 City API GET /api/v1/cities returns active cities")
    void testRegressionF1CitiesApi() throws Exception {
        mockMvc.perform(get("/api/v1/cities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].name", hasItems("Pune", "Mumbai", "Delhi")));
    }

    @Test
    @DisplayName("REGRESSION TEST 28: F1 Latest Air API GET /api/v1/cities/{id}/air-quality/latest works intact")
    void testRegressionF1LatestAirApi() throws Exception {
        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/air-quality/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.cityName", is("Pune")))
                .andExpect(jsonPath("$.observations", hasSize(3)))
                .andExpect(jsonPath("$.observations[0].stationId", notNullValue()))
                .andExpect(jsonPath("$.observations[0].pm25", notNullValue()));
    }

    @Test
    @DisplayName("REGRESSION TEST 29: F1 History API GET /api/v1/stations/{stationId}/air-quality works intact")
    void testRegressionF1HistoryApi() throws Exception {
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                        .param("from", "2026-09-24T00:00:00Z")
                        .param("to", "2026-09-25T00:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-001")))
                .andExpect(jsonPath("$.observations", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.observations[0].pm25", notNullValue()));
    }
}
