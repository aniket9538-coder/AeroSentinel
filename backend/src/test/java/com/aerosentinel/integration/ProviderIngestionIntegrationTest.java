package com.aerosentinel.integration;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.integration.cpcb.CpcbClient;
import com.aerosentinel.integration.provider.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private AirObservationRepository airObservationRepository;

    @Autowired
    private CpcbClient cpcbClient;

    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_TIMESTAMP_STR = "2026-09-25T03:00:00Z";
    private static final Instant TEST_TIMESTAMP = Instant.parse(TEST_TIMESTAMP_STR);

    @AfterEach
    void tearDown() {
        // Clean up only test-ingested records so they don't affect other tests or wipe real historical data
        List<AirObservation> openAqRecords = airObservationRepository.findAll().stream()
                .filter(o -> "OPENAQ".equalsIgnoreCase(o.getSource())
                        && o.getObservedAt() != null
                        && o.getObservedAt().isAfter(Instant.parse("2026-09-25T00:00:00Z")))
                .toList();
        if (!openAqRecords.isEmpty()) {
            airObservationRepository.deleteAll(openAqRecords);
        }
    }

    @Test
    @DisplayName("TEST 1: Valid external PM2.5 record is persisted to PostgreSQL")
    void testValidRecordPersisted() {
        ProviderObservation obs = new ProviderObservation(
                "8118", 18.5314, 73.8446, TEST_TIMESTAMP, 52.5, "OPENAQ", "VALID");

        IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(obs));

        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getMapped()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(1);
        assertThat(summary.getDuplicates()).isEqualTo(0);
        assertThat(summary.getRejected()).isEqualTo(0);

        // Verify direct database query
        boolean existsInDb = airObservationRepository.existsByStationIdAndObservedAt("PUN-001", TEST_TIMESTAMP);
        assertThat(existsInDb).isTrue();
    }

    @Test
    @DisplayName("TEST 2: Invalid PM2.5 value is rejected without persistence")
    void testInvalidPm25Rejected() {
        Instant testTime = Instant.parse("2026-09-25T03:15:00Z");
        ProviderObservation obs = new ProviderObservation(
                "8118", 18.5314, 73.8446, testTime, -10.0, "OPENAQ", "VALID");

        IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(obs));

        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getRejected()).isEqualTo(1);
        assertThat(summary.getRejectionReasons()).contains(IngestionService.REASON_INVALID_PM25);

        boolean existsInDb = airObservationRepository.existsByStationIdAndObservedAt("PUN-001", testTime);
        assertThat(existsInDb).isFalse();
    }

    @Test
    @DisplayName("TEST 3: Invalid/null timestamp is rejected without persistence")
    void testInvalidTimestampRejected() {
        ProviderObservation obs = new ProviderObservation(
                "8118", 18.5314, 73.8446, null, 45.0, "OPENAQ", "VALID");

        IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(obs));

        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getRejected()).isEqualTo(1);
        assertThat(summary.getRejectionReasons()).contains(IngestionService.REASON_INVALID_TIMESTAMP);
    }

    @Test
    @DisplayName("TEST 4: Unmapped station is rejected without persistence")
    void testUnmappedStationRejected() {
        Instant testTime = Instant.parse("2026-09-25T03:30:00Z");
        ProviderObservation obs = new ProviderObservation(
                "UNKNOWN_STATION_999", 12.9716, 77.5946, testTime, 55.0, "OPENAQ", "VALID");

        IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(obs));

        assertThat(summary.getFetched()).isEqualTo(1);
        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getRejected()).isEqualTo(1);
        assertThat(summary.getRejectionReasons()).contains(IngestionService.REASON_UNMAPPED_STATION);
    }

    @Test
    @DisplayName("TEST 5: Duplicate observation is detected and not inserted twice")
    void testDuplicatePrevention() {
        Instant testTime = Instant.parse("2026-09-25T03:45:00Z");
        ProviderObservation obs = new ProviderObservation(
                "8118", 18.5314, 73.8446, testTime, 48.0, "OPENAQ", "VALID");

        // First ingestion: should insert
        IngestionSummary summary1 = ingestionService.processObservations("OPENAQ", List.of(obs));
        assertThat(summary1.getInserted()).isEqualTo(1);
        assertThat(summary1.getDuplicates()).isEqualTo(0);

        // Second ingestion: must detect duplicate and NOT insert
        IngestionSummary summary2 = ingestionService.processObservations("OPENAQ", List.of(obs));
        assertThat(summary2.getInserted()).isEqualTo(0);
        assertThat(summary2.getDuplicates()).isEqualTo(1);
    }

    @Test
    @DisplayName("TEST 6: Provider unavailable does not create fake data")
    void testProviderUnavailableCreatesNoFakeData() {
        // CPCB client is configured to be unavailable
        IngestionSummary summary = ingestionService.ingest(cpcbClient);

        assertThat(summary.getStatus()).isEqualTo(ProviderStatus.NOT_CONFIGURED);
        assertThat(summary.getFetched()).isEqualTo(0);
        assertThat(summary.getInserted()).isEqualTo(0);
    }

    @Test
    @DisplayName("TEST 7: Empty provider response creates no fake observations")
    void testEmptyProviderResponse() {
        IngestionSummary summary = ingestionService.processObservations("OPENAQ", Collections.emptyList());

        assertThat(summary.getStatus()).isEqualTo(ProviderStatus.EMPTY_RESPONSE);
        assertThat(summary.getFetched()).isEqualTo(0);
        assertThat(summary.getInserted()).isEqualTo(0);
    }

    @Test
    @DisplayName("TEST 8: Existing Phase 3 APIs return ingested observations correctly")
    void testPhase3ApiReturnsIngestedObservation() throws Exception {
        // Ingest a recent observation for PUN-001
        Instant testTime = Instant.parse("2026-09-25T04:00:00Z");
        ProviderObservation obs = new ProviderObservation(
                "8118", 18.5314, 73.8446, testTime, 99.5, "OPENAQ", "VALID");

        IngestionSummary summary = ingestionService.processObservations("OPENAQ", List.of(obs));
        assertThat(summary.getInserted()).isEqualTo(1);

        // Verify latest air-quality API includes this latest observation
        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/air-quality/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-001')].pm25", contains(99.5)))
                .andExpect(jsonPath("$.observations[?(@.stationId == 'PUN-001')].source", contains("OPENAQ")));

        // Verify station history API also includes it
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                        .param("from", "2026-09-25T03:00:00Z")
                        .param("to", "2026-09-25T05:00:00Z")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-001")))
                .andExpect(jsonPath("$.observations", hasSize(1)))
                .andExpect(jsonPath("$.observations[0].pm25", is(99.5)))
                .andExpect(jsonPath("$.observations[0].source", is("OPENAQ")));
    }

    @Test
    @DisplayName("TEST 9: Source provenance is preserved correctly as OPENAQ")
    void testSourcePreservedCorrectly() {
        Instant testTime = Instant.parse("2026-09-25T04:30:00Z");
        ProviderObservation obs = new ProviderObservation(
                "8119", 18.4575, 73.8677, testTime, 65.0, "OPENAQ", "VALID");

        ingestionService.processObservations("OPENAQ", List.of(obs));

        AirObservation entity = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-002")
                .stream()
                .filter(o -> testTime.equals(o.getObservedAt()))
                .findFirst()
                .orElseThrow();

        assertThat(entity.getSource()).isEqualTo("OPENAQ");
        assertThat(entity.getPm25()).isEqualTo(65.0);
    }

    @Test
    @DisplayName("TEST 10: Observed timestamp remains exact provider measurement timestamp")
    void testObservedTimestampRemainsExactMeasurementTime() {
        Instant providerMeasurementTime = Instant.parse("2026-09-25T04:45:00Z");
        ProviderObservation obs = new ProviderObservation(
                "8120", 18.5089, 73.9260, providerMeasurementTime, 72.0, "OPENAQ", "VALID");

        ingestionService.processObservations("OPENAQ", List.of(obs));

        AirObservation entity = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-003")
                .stream()
                .filter(o -> providerMeasurementTime.equals(o.getObservedAt()))
                .findFirst()
                .orElseThrow();

        assertThat(entity.getObservedAt()).isEqualTo(providerMeasurementTime);
        assertThat(entity.getCreatedAt()).isAfter(providerMeasurementTime); // IngestedAt is distinguished from ObservedAt
    }
}
