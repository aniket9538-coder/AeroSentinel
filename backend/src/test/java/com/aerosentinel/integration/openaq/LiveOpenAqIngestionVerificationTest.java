package com.aerosentinel.integration.openaq;

import com.aerosentinel.air.AirObservation;
import com.aerosentinel.air.AirObservationRepository;
import com.aerosentinel.integration.provider.IngestionService;
import com.aerosentinel.integration.provider.IngestionSummary;
import com.aerosentinel.integration.provider.ProviderFetchResult;
import com.aerosentinel.integration.provider.ProviderObservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "OPENAQ_API_KEY", matches = ".+")
class LiveOpenAqIngestionVerificationTest {

    private static final Logger log = LoggerFactory.getLogger(LiveOpenAqIngestionVerificationTest.class);
    private static final String PUNE_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PUNE_SHIVAJINAGAR_LOCATION_ID = "11613";

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
        List<AirObservation> testRecords = airObservationRepository.findAll().stream()
                .filter(o -> o.getCreatedAt() != null
                        && o.getCreatedAt().isAfter(Instant.parse("2026-09-25T18:14:00Z")))
                .toList();
        if (!testRecords.isEmpty()) {
            airObservationRepository.deleteAll(testRecords);
        }
    }

    @Test
    @DisplayName("LIVE VERIFICATION: End-to-end live OpenAQ request -> Mapper -> StationResolver -> PostgreSQL -> Existing APIs")
    void testLiveOpenAqPipelineEndToEnd() throws Exception {
        // 1. Confirm Provider is configured
        assertThat(openAqClient.isConfigured()).isTrue();
        assertThat(openAqClient.getProviderName()).isEqualTo("OPENAQ");

        // 2. Fetch live data from OpenAQ for Pune Shivajinagar
        ProviderFetchResult fetchResult = openAqClient.fetchLatestObservations(PUNE_SHIVAJINAGAR_LOCATION_ID);
        assertThat(fetchResult.isSuccessful()).isTrue();
        assertThat(fetchResult.getObservations()).isNotEmpty();

        ProviderObservation liveObs = fetchResult.getObservations().stream()
                .filter(o -> "pm25".equalsIgnoreCase(o.getRawParameter()))
                .filter(o -> o.getObservedAt().toString().startsWith("2026")) // latest active 2026 sensor
                .findFirst()
                .orElse(fetchResult.getObservations().get(0));

        log.info("LIVE PROVIDER EVIDENCE: stationId={}, observedAt={}, pm25={}, source={}, unit={}",
                liveObs.getProviderStationId(), liveObs.getObservedAt(), liveObs.getPm25(),
                liveObs.getSource(), liveObs.getUnit());

        assertThat(liveObs.getPm25()).isNotNull();
        assertThat(liveObs.getPm25()).isGreaterThanOrEqualTo(0.0);
        assertThat(liveObs.getObservedAt()).isNotNull();
        assertThat(liveObs.getSource()).isEqualTo("OPENAQ");

        long countBefore = airObservationRepository.count();

        // 3. First Ingestion: Run live ingestion through pipeline
        IngestionSummary summary1 = ingestionService.ingest(openAqClient, PUNE_SHIVAJINAGAR_LOCATION_ID);
        assertThat(summary1.getInserted()).isGreaterThanOrEqualTo(1);
        assertThat(summary1.getDuplicates()).isEqualTo(0);

        long countAfterFirst = airObservationRepository.count();
        assertThat(countAfterFirst).isEqualTo(countBefore + summary1.getInserted());

        // 4. PostgreSQL Direct Verification
        AirObservation dbEntity = airObservationRepository.findByStationIdOrderByObservedAtDesc("PUN-001").stream()
                .filter(o -> liveObs.getObservedAt().equals(o.getObservedAt()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Observation not found in PostgreSQL"));

        assertThat(dbEntity.getStationId()).isEqualTo("PUN-001");
        assertThat(dbEntity.getPm25()).isEqualTo(liveObs.getPm25());
        assertThat(dbEntity.getObservedAt()).isEqualTo(liveObs.getObservedAt());
        assertThat(dbEntity.getSource()).isEqualTo("OPENAQ");
        assertThat(dbEntity.getDataQuality()).isEqualTo("VALID");

        // 5. Duplicate Verification: Run same live ingestion again
        IngestionSummary summary2 = ingestionService.ingest(openAqClient, PUNE_SHIVAJINAGAR_LOCATION_ID);
        assertThat(summary2.getInserted()).isEqualTo(0);
        assertThat(summary2.getDuplicates()).isGreaterThanOrEqualTo(1);

        long countAfterSecond = airObservationRepository.count();
        assertThat(countAfterSecond).isEqualTo(countAfterFirst); // Row count must NOT increase

        // 6. Existing Phase 3 API Verification
        // A. City Latest Endpoint returns HTTP 200 without breaking contract
        mockMvc.perform(get("/api/v1/cities/" + PUNE_ID + "/air-quality/latest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId", is(PUNE_ID)))
                .andExpect(jsonPath("$.cityName", is("Pune")))
                .andExpect(jsonPath("$.observations", hasSize(3)));

        // B. Station History Endpoint returns the exact ingested live observation
        mockMvc.perform(get("/api/v1/stations/PUN-001/air-quality")
                        .param("from", liveObs.getObservedAt().minusSeconds(60).toString())
                        .param("to", liveObs.getObservedAt().plusSeconds(60).toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId", is("PUN-001")))
                .andExpect(jsonPath("$.observations", hasSize(1)))
                .andExpect(jsonPath("$.observations[0].pm25", is(liveObs.getPm25())))
                .andExpect(jsonPath("$.observations[0].observedAt", is(liveObs.getObservedAt().toString())))
                .andExpect(jsonPath("$.observations[0].source", is("OPENAQ")))
                .andExpect(jsonPath("$.observations[0].quality", is("VALID")));
    }
}
