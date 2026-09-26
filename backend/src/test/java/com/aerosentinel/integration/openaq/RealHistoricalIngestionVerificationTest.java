package com.aerosentinel.integration.openaq;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.integration.provider.IngestionService;
import com.aerosentinel.integration.provider.IngestionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RealHistoricalIngestionVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(RealHistoricalIngestionVerificationTest.class);

    @Autowired
    private OpenAqClient openAqClient;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private AirObservationRepository airObservationRepository;

    @Autowired
    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void cleanOpenAqRecords() {
        java.util.List<AirObservation> testRecords = airObservationRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null
                        && o.getCreatedAt().isAfter(java.time.Instant.parse("2026-09-25T18:14:00Z")))
                .toList();
        if (!testRecords.isEmpty()) {
            airObservationRepository.deleteAll(testRecords);
        }
    }

    // Verified real sensor IDs and location IDs from OpenAQ
    private static final Map<String, String[]> STATIONS_TO_INGEST = Map.of(
            "MUM-001", new String[]{"6945", "12235834"},
            "MUM-002", new String[]{"6948", "12235860"},
            "DEL-001", new String[]{"17", "12234787"},
            "DEL-002", new String[]{"235", "12235610"},
            "DEL-003", new String[]{"50", "12234796"}
    );

    @Test
    @DisplayName("INGEST & VERIFY: Real 24H PM2.5 history ingestion for Mumbai and Delhi with duplicate protection")
    void testRealHistoricalPm25Ingestion() throws Exception {
        assertThat(openAqClient.isConfigured()).isTrue();

        Instant from = Instant.parse("2026-09-23T15:30:00Z");
        Instant to = Instant.parse("2026-09-24T17:30:00Z");

        // 1. Ingest real 24H history for Mumbai and Delhi
        for (Map.Entry<String, String[]> entry : STATIONS_TO_INGEST.entrySet()) {
            String stationCode = entry.getKey();
            String locId = entry.getValue()[0];
            String sensorId = entry.getValue()[1];

            log.info("Ingesting real 24H history for station {} (locId={}, sensorId={})...", stationCode, locId, sensorId);
            IngestionSummary summary = ingestionService.ingestSensorHours(openAqClient, locId, sensorId, from, to);

            log.info("Station {} ingestion summary: fetched={}, mapped={}, inserted={}, duplicates={}, rejected={}",
                    stationCode, summary.getFetched(), summary.getMapped(), summary.getInserted(),
                    summary.getDuplicates(), summary.getRejected());

            assertThat(summary.getFetched()).isGreaterThanOrEqualTo(20);
            assertThat(summary.getInserted() + summary.getDuplicates()).isEqualTo(summary.getFetched());
        }

        // 2. Verify Duplicate Safety by re-running ingestion
        for (Map.Entry<String, String[]> entry : STATIONS_TO_INGEST.entrySet()) {
            String stationCode = entry.getKey();
            String locId = entry.getValue()[0];
            String sensorId = entry.getValue()[1];

            IngestionSummary dupSummary = ingestionService.ingestSensorHours(openAqClient, locId, sensorId, from, to);
            log.info("Station {} duplicate re-run summary: inserted={}, duplicates={}",
                    stationCode, dupSummary.getInserted(), dupSummary.getDuplicates());

            assertThat(dupSummary.getInserted()).isEqualTo(0);
            assertThat(dupSummary.getDuplicates()).isGreaterThanOrEqualTo(20);
        }

        // 3. Verify Database Counts
        for (String stationCode : STATIONS_TO_INGEST.keySet()) {
            List<AirObservation> history = airObservationRepository.findByStationIdAndObservedAtBetweenOrderByObservedAtAsc(
                    stationCode, from, to);

            log.info("Station {} in DB between {} and {}: count={}", stationCode, from, to, history.size());
            assertThat(history.size()).isGreaterThanOrEqualTo(24);

            // Confirm real provenance
            for (AirObservation obs : history) {
                assertThat(obs.getSource()).isEqualTo("OPENAQ");
                assertThat(obs.getPm25()).isNotNull().isGreaterThan(0.0);
                assertThat(obs.getDataQuality()).isEqualTo("VALID");
                assertThat(obs.getStationId()).isEqualTo(stationCode);
            }
        }

        // 4. Confirm Pune remains untouched (12 observations each)
        for (String puneStation : List.of("PUN-001", "PUN-002", "PUN-003")) {
            List<AirObservation> puneObs = airObservationRepository.findByStationIdOrderByObservedAtDesc(puneStation);
            log.info("Pune station {} in DB: total count={}", puneStation, puneObs.size());
            assertThat(puneObs).hasSize(12);
        }

        // 5. Verify Canonical History API for MUM-001 and DEL-001
        mockMvc.perform(get("/api/v1/stations/MUM-001/air-quality")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("MUM-001")))
                .andExpect(jsonPath("$.observations", hasSize(greaterThanOrEqualTo(24))))
                .andExpect(jsonPath("$.observations[0].source", is("OPENAQ")));

        mockMvc.perform(get("/api/v1/stations/DEL-001/air-quality")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("DEL-001")))
                .andExpect(jsonPath("$.observations", hasSize(greaterThanOrEqualTo(24))))
                .andExpect(jsonPath("$.observations[0].source", is("OPENAQ")));

        log.info("Real historical PM2.5 verification PASSED completely for Mumbai and Delhi!");
    }
}
