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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MultiCityOpenAqLiveVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(MultiCityOpenAqLiveVerificationTest.class);

    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String MUMBAI_ID = "550e8400-e29b-41d4-a716-446655440002";
    private static final String DELHI_ID = "550e8400-e29b-41d4-a716-446655440003";

    // Verified OpenAQ Location IDs
    private static final String MUMBAI_KURLA_LOC = "6945";
    private static final String MUMBAI_AIRPORT_LOC = "6948";

    private static final String DELHI_RK_PURAM_LOC = "17";
    private static final String DELHI_ANAND_VIHAR_LOC = "235";
    private static final String DELHI_PUNJABI_BAGH_LOC = "50";

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

    @Test
    @DisplayName("LIVE VERIFICATION: Real multi-city ingestion for Mumbai and Delhi with duplicate protection and canonical API verification")
    void testMultiCityIngestionAndApis() throws Exception {
        assertThat(openAqClient.isConfigured()).isTrue();

        // 1. Ingest Mumbai real observations
        log.info("Ingesting Mumbai real observations from OpenAQ...");
        IngestionSummary mumSummary1 = ingestionService.ingest(openAqClient, MUMBAI_KURLA_LOC);
        IngestionSummary mumSummary2 = ingestionService.ingest(openAqClient, MUMBAI_AIRPORT_LOC);

        log.info("Mumbai Kurla ingestion: inserted={}, duplicates={}", mumSummary1.getInserted(), mumSummary1.getDuplicates());
        log.info("Mumbai Airport ingestion: inserted={}, duplicates={}", mumSummary2.getInserted(), mumSummary2.getDuplicates());

        assertThat(mumSummary1.getInserted() + mumSummary1.getDuplicates()).isGreaterThanOrEqualTo(1);
        assertThat(mumSummary2.getInserted() + mumSummary2.getDuplicates()).isGreaterThanOrEqualTo(1);

        // 2. Ingest Delhi real observations
        log.info("Ingesting Delhi real observations from OpenAQ...");
        IngestionSummary delSummary1 = ingestionService.ingest(openAqClient, DELHI_RK_PURAM_LOC);
        IngestionSummary delSummary2 = ingestionService.ingest(openAqClient, DELHI_ANAND_VIHAR_LOC);
        IngestionSummary delSummary3 = ingestionService.ingest(openAqClient, DELHI_PUNJABI_BAGH_LOC);

        log.info("Delhi RK Puram ingestion: inserted={}, duplicates={}", delSummary1.getInserted(), delSummary1.getDuplicates());
        log.info("Delhi Anand Vihar ingestion: inserted={}, duplicates={}", delSummary2.getInserted(), delSummary2.getDuplicates());
        log.info("Delhi Punjabi Bagh ingestion: inserted={}, duplicates={}", delSummary3.getInserted(), delSummary3.getDuplicates());

        assertThat(delSummary1.getInserted() + delSummary1.getDuplicates()).isGreaterThanOrEqualTo(1);
        assertThat(delSummary2.getInserted() + delSummary2.getDuplicates()).isGreaterThanOrEqualTo(1);
        assertThat(delSummary3.getInserted() + delSummary3.getDuplicates()).isGreaterThanOrEqualTo(1);

        // 3. Duplicate Protection Check: Re-run ingestion and assert 0 inserted, only duplicates
        IngestionSummary dupCheck = ingestionService.ingest(openAqClient, MUMBAI_KURLA_LOC);
        assertThat(dupCheck.getInserted()).isEqualTo(0);
        assertThat(dupCheck.getDuplicates()).isGreaterThanOrEqualTo(1);

        // 4. Verify PostgreSQL Database Records
        // Mumbai
        List<AirObservation> mumKurlaObs = airObservationRepository.findByStationIdOrderByObservedAtDesc("MUM-001");
        List<AirObservation> mumAirportObs = airObservationRepository.findByStationIdOrderByObservedAtDesc("MUM-002");
        assertThat(mumKurlaObs).isNotEmpty();
        assertThat(mumAirportObs).isNotEmpty();

        AirObservation mumObs = mumKurlaObs.get(0);
        assertThat(mumObs.getSource()).isEqualTo("OPENAQ");
        assertThat(mumObs.getPm25()).isNotNull().isGreaterThan(0.0);
        assertThat(mumObs.getObservedAt()).isNotNull();

        // Delhi
        List<AirObservation> delRkPuramObs = airObservationRepository.findByStationIdOrderByObservedAtDesc("DEL-001");
        List<AirObservation> delAnandViharObs = airObservationRepository.findByStationIdOrderByObservedAtDesc("DEL-002");
        List<AirObservation> delPunjabiBaghObs = airObservationRepository.findByStationIdOrderByObservedAtDesc("DEL-003");
        assertThat(delRkPuramObs).isNotEmpty();
        assertThat(delAnandViharObs).isNotEmpty();
        assertThat(delPunjabiBaghObs).isNotEmpty();

        AirObservation delObs = delRkPuramObs.get(0);
        assertThat(delObs.getSource()).isEqualTo("OPENAQ");
        assertThat(delObs.getPm25()).isNotNull().isGreaterThan(0.0);
        assertThat(delObs.getObservedAt()).isNotNull();

        // Pune Baseline Preservation Check (Pune observations remain intact)
        List<AirObservation> pun1 = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-001");
        List<AirObservation> pun2 = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-002");
        List<AirObservation> pun3 = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-003");
        assertThat(pun1).isNotEmpty();
        assertThat(pun2).isNotEmpty();
        assertThat(pun3).isNotEmpty();

        // 5. Canonical API Verification
        // Pune
        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/air-quality/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.cityName", is("Pune")))
                .andExpect(jsonPath("$.observations", hasSize(3)));

        // Mumbai
        mockMvc.perform(get("/api/v1/cities/" + MUMBAI_ID + "/air-quality/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(MUMBAI_ID)))
                .andExpect(jsonPath("$.cityName", is("Mumbai")))
                .andExpect(jsonPath("$.observations", hasSize(2)))
                .andExpect(jsonPath("$.observations[*].stationId", hasItems("MUM-001", "MUM-002")))
                .andExpect(jsonPath("$.observations[0].source", is("OPENAQ")))
                .andExpect(jsonPath("$.observations[0].pm25", notNullValue()));

        // Delhi
        mockMvc.perform(get("/api/v1/cities/" + DELHI_ID + "/air-quality/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(DELHI_ID)))
                .andExpect(jsonPath("$.cityName", is("Delhi")))
                .andExpect(jsonPath("$.observations", hasSize(3)))
                .andExpect(jsonPath("$.observations[*].stationId", hasItems("DEL-001", "DEL-002", "DEL-003")))
                .andExpect(jsonPath("$.observations[0].source", is("OPENAQ")))
                .andExpect(jsonPath("$.observations[0].pm25", notNullValue()));

        // Station History API for Mumbai
        mockMvc.perform(get("/api/v1/stations/MUM-001/air-quality")
                        .param("from", mumObs.getObservedAt().minusSeconds(3600).toString())
                        .param("to", mumObs.getObservedAt().plusSeconds(3600).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("MUM-001")))
                .andExpect(jsonPath("$.observations", not(empty())));

        // Station History API for Delhi
        mockMvc.perform(get("/api/v1/stations/DEL-001/air-quality")
                        .param("from", delObs.getObservedAt().minusSeconds(3600).toString())
                        .param("to", delObs.getObservedAt().plusSeconds(3600).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("DEL-001")))
                .andExpect(jsonPath("$.observations", not(empty())));

        log.info("Multi-city real air quality verification PASSED completely for Pune, Mumbai, and Delhi!");
    }
}
