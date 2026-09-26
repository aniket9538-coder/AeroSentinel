package com.aerosentinel.grid;

import com.aerosentinel.spatial.H3Service;
import com.uber.h3core.util.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F2 Phase 5 Contract Test: Canonical Grid REST APIs.
 * Endpoints:
 * - GET /api/v1/grid?cityId={cityId}
 * - GET /api/v1/grid/{h3Index}
 */
@SpringBootTest
@AutoConfigureMockMvc
class F2GridApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GridRepository gridRepository;

    @Autowired
    private H3Service h3Service;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String MUMBAI_ID = "550e8400-e29b-41d4-a716-446655440002";
    private static final String DELHI_ID = "550e8400-e29b-41d4-a716-446655440003";
    private static final String UNKNOWN_CITY_ID = "00000000-0000-0000-0000-000000000000";
    private static final String PUNE_SAMPLE_H3 = "88608850e5fffff"; // Pune Shivajinagar
    private static final String UNKNOWN_VALID_H3 = "882681a339fffff"; // Valid H3 cell (San Francisco), not in DB

    @Test
    @DisplayName("GRID TEST 8: GET /api/v1/grid?cityId={puneId} returns HTTP 200 with real Pune grid cells")
    void testValidPuneGrid() throws Exception {
        mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", PUNE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].h3Index", hasItem(PUNE_SAMPLE_H3)))
                .andExpect(jsonPath("$[0].cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$[0].resolution", is(8)))
                .andExpect(jsonPath("$[0].center.lat", notNullValue()))
                .andExpect(jsonPath("$[0].center.lng", notNullValue()))
                .andExpect(jsonPath("$[0].boundary", hasSize(6)));
    }

    @Test
    @DisplayName("GRID TEST 9: GET /api/v1/grid?cityId={mumbaiId} returns HTTP 200 with real Mumbai grid cells")
    void testValidMumbaiGrid() throws Exception {
        mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", MUMBAI_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].cityId", is(MUMBAI_ID)))
                .andExpect(jsonPath("$[0].resolution", is(8)))
                .andExpect(jsonPath("$[0].center.lat", notNullValue()))
                .andExpect(jsonPath("$[0].boundary", hasSize(6)));
    }

    @Test
    @DisplayName("GRID TEST 10: GET /api/v1/grid?cityId={delhiId} returns HTTP 200 with real Delhi grid cells")
    void testValidDelhiGrid() throws Exception {
        mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", DELHI_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].cityId", is(DELHI_ID)))
                .andExpect(jsonPath("$[0].resolution", is(8)))
                .andExpect(jsonPath("$[0].boundary", hasSize(6)));
    }

    @Test
    @DisplayName("GRID TEST 11: Malformed city UUID in /api/v1/grid returns HTTP 400 BAD_REQUEST")
    void testMalformedCityUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", "not-a-valid-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("GRID TEST 12: Unknown city UUID in /api/v1/grid returns HTTP 404 NOT_FOUND")
    void testUnknownCityUuidReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", UNKNOWN_CITY_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("GRID TEST 13: Returned cells match database grid_cells records exactly")
    void testReturnedCellsMatchDatabase() throws Exception {
        UUID puneUuid = UUID.fromString(PUNE_ID);
        List<GridCell> dbCells = gridRepository.findByCityId(puneUuid);
        List<String> dbH3Indices = dbCells.stream().map(GridCell::getH3Index).toList();

        MvcResult result = mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", PUNE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> responseList = objectMapper.readValue(
                result.getResponse().getContentAsString(), List.class);

        assertThat(responseList).hasSameSizeAs(dbCells);
        List<String> responseH3Indices = responseList.stream()
                .map(m -> (String) m.get("h3Index"))
                .toList();

        assertThat(responseH3Indices).containsExactlyInAnyOrderElementsOf(dbH3Indices);
    }

    @Test
    @DisplayName("GRID TEST 14: No duplicate cells returned for city")
    void testNoDuplicateCellsReturned() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/grid")
                        .param("cityId", PUNE_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> responseList = objectMapper.readValue(
                result.getResponse().getContentAsString(), List.class);

        List<String> h3Indices = responseList.stream()
                .map(m -> (String) m.get("h3Index"))
                .toList();

        assertThat(h3Indices).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("CELL TEST 15: Valid H3 cell in GET /api/v1/grid/{h3Index} returns HTTP 200 with details")
    void testValidH3CellDetails() throws Exception {
        mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.h3Index", is(PUNE_SAMPLE_H3)))
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.resolution", is(8)))
                .andExpect(jsonPath("$.center.lat", notNullValue()))
                .andExpect(jsonPath("$.center.lng", notNullValue()))
                .andExpect(jsonPath("$.boundary", hasSize(6)));
    }

    @Test
    @DisplayName("CELL TEST 16: Malformed H3 string in /api/v1/grid/{h3Index} returns HTTP 400 BAD_REQUEST")
    void testMalformedH3Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/grid/invalid-h3-string")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("CELL TEST 17: Valid H3 syntax but unpersisted cell in /api/v1/grid/{h3Index} returns HTTP 404 NOT_FOUND")
    void testUnknownH3Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/grid/" + UNKNOWN_VALID_H3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("CELL TEST 18: Center coordinates match authentic H3 centroid calculation")
    void testCenterCorrectness() throws Exception {
        LatLng expectedCenter = h3Service.h3ToCenter(PUNE_SAMPLE_H3);

        mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.center.lat", closeTo(expectedCenter.lat, 0.0001)))
                .andExpect(jsonPath("$.center.lng", closeTo(expectedCenter.lng, 0.0001)));
    }

    @Test
    @DisplayName("CELL TEST 19: Boundary vertices match authentic H3 hexagon perimeter coordinates")
    void testBoundaryCorrectness() throws Exception {
        List<LatLng> expectedBoundary = h3Service.h3ToBoundary(PUNE_SAMPLE_H3);
        assertThat(expectedBoundary).hasSize(6);

        mockMvc.perform(get("/api/v1/grid/" + PUNE_SAMPLE_H3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boundary", hasSize(6)))
                .andExpect(jsonPath("$.boundary[0].lat", closeTo(expectedBoundary.get(0).lat, 0.0001)))
                .andExpect(jsonPath("$.boundary[0].lng", closeTo(expectedBoundary.get(0).lng, 0.0001)));
    }
}
