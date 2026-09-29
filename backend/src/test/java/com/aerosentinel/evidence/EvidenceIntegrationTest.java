package com.aerosentinel.evidence;

import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvidenceIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(EvidenceIntegrationTest.class);
    private static final String REAL_PUNE_H3 = "88608850e5fffff";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GeminiAnalysisRepository geminiAnalysisRepository;

    @Autowired
    private PollutionEventRepository pollutionEventRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Test
    @Order(1)
    @DisplayName("P3-E2E: Real Pune H3 end-to-end evidence chain with event, evidence, and Gemini persistence")
    void testRealPuneEvidenceOrchestration() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/hotspot/" + REAL_PUNE_H3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // 1. Context tier
                .andExpect(jsonPath("$.context.h3Index").value(REAL_PUNE_H3))
                .andExpect(jsonPath("$.context.predictionId").isNotEmpty())
                .andExpect(jsonPath("$.context.eventId").isNotEmpty())
                .andExpect(jsonPath("$.context.eventCode").isNotEmpty())
                .andExpect(jsonPath("$.context.cityName").value("Pune"))
                // 2. Observed facts tier
                .andExpect(jsonPath("$.observedFacts.air").exists())
                .andExpect(jsonPath("$.observedFacts.weather").exists())
                .andExpect(jsonPath("$.observedFacts.monitoringCoverage").exists())
                // 3. Model outputs tier
                .andExpect(jsonPath("$.modelOutputs.hotspot.isHotspot").value(true))
                .andExpect(jsonPath("$.modelOutputs.hotspot.operationalThreshold").value(0.20))
                .andExpect(jsonPath("$.modelOutputs.hotspot.modelVersion").value("hotspot_classifier_v1"))
                .andExpect(jsonPath("$.modelOutputs.hotspot.riskScore", greaterThan(0.0)))
                .andExpect(jsonPath("$.modelOutputs.forecast.parentPredictionId").isNotEmpty())
                // 4. Evidence & Triage tier
                .andExpect(jsonPath("$.evidence.evidenceScore", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.evidence.triageState", in(List.of("ALERT_CANDIDATE", "MONITOR", "INSUFFICIENT_EVIDENCE"))))
                .andExpect(jsonPath("$.evidence.signals", not(empty())))
                .andExpect(jsonPath("$.evidence.clusterH3Cells", hasItem(REAL_PUNE_H3)))
                // 5. AI Interpretation tier
                .andExpect(jsonPath("$.aiInterpretation.summaryPublic").isNotEmpty())
                .andExpect(jsonPath("$.aiInterpretation.isGrounded").value(true))
                .andExpect(jsonPath("$.aiInterpretation.causalClaimSupported").value(false))
                // 6. Recommended Verification tier
                .andExpect(jsonPath("$.recommendedVerification.action").isNotEmpty())
                .andExpect(jsonPath("$.recommendedVerification.priority").isNotEmpty())
                // 7. Provenance tier
                .andExpect(jsonPath("$.provenance.f3ModelVersion").value("hotspot_classifier_v1"))
                .andExpect(jsonPath("$.provenance.h3Index").value(REAL_PUNE_H3));

        // 1. Verify PollutionEvent persistence in PostgreSQL
        List<PollutionEvent> events = pollutionEventRepository.findByH3IndexOrderByStartedAtDesc(REAL_PUNE_H3);
        assertThat(events).isNotEmpty();
        PollutionEvent latestEvent = events.get(0);
        assertThat(latestEvent.getH3Index()).isEqualTo(REAL_PUNE_H3);
        assertThat(latestEvent.getPredictionId()).isNotNull();
        assertThat(latestEvent.getEventCode()).startsWith("EVT-");
        assertThat(latestEvent.getGridCellId()).isNotNull();
        log.info("Verified PollutionEvent record: id={}, code={}, h3={}, predictionId={}",
                latestEvent.getId(), latestEvent.getEventCode(), latestEvent.getH3Index(), latestEvent.getPredictionId());

        // 2. Verify EventEvidence persistence in PostgreSQL
        List<EventEvidence> evidenceItems = evidenceRepository.findByEventId(latestEvent.getId());
        assertThat(evidenceItems).isNotEmpty();
        for (EventEvidence ee : evidenceItems) {
            assertThat(ee.getEventId()).isEqualTo(latestEvent.getId());
            assertThat(ee.getSourceType()).isNotEmpty();
            assertThat(ee.getEvidenceKey()).isNotEmpty();
            assertThat(ee.getEvidenceValue()).isNotEmpty();
        }
        log.info("Verified EventEvidence records count={} for eventId={}",
                evidenceItems.size(), latestEvent.getId());

        // 3. Verify GeminiAnalysis persistence in PostgreSQL
        List<GeminiAnalysis> persisted = geminiAnalysisRepository.findByPredictionIdOrderByCreatedAtDesc(latestEvent.getPredictionId());
        assertThat(persisted).isNotEmpty();
        GeminiAnalysis latestAnalysis = persisted.get(0);
        assertThat(latestAnalysis.getH3Index()).isEqualTo(REAL_PUNE_H3);
        assertThat(latestAnalysis.getEventId()).isEqualTo(latestEvent.getId());
        assertThat(latestAnalysis.getPredictionId()).isEqualTo(latestEvent.getPredictionId());
        assertThat(latestAnalysis.getEventSummaryPublic()).isNotEmpty();
        assertThat(latestAnalysis.getIsGrounded()).isTrue();
        log.info("Verified GeminiAnalysis record: id={}, eventId={}, predictionId={}, h3={}",
                latestAnalysis.getId(), latestAnalysis.getEventId(), latestAnalysis.getPredictionId(), latestAnalysis.getH3Index());
    }

    @Test
    @Order(2)
    @DisplayName("P3-E2E: Duplicate request for same H3 cell does not duplicate EventEvidence records")
    void testDuplicateRequestDoesNotDuplicateEvidence() throws Exception {
        // Query initial count of evidence for the Pune cell's active event
        List<PollutionEvent> events = pollutionEventRepository.findByH3IndexOrderByStartedAtDesc(REAL_PUNE_H3);
        assertThat(events).isNotEmpty();
        UUID eventId = events.get(0).getId();
        int initialEvidenceCount = evidenceRepository.findByEventId(eventId).size();

        // Perform second request
        mockMvc.perform(get("/api/v1/evidence/hotspot/" + REAL_PUNE_H3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Evidence count must remain identical (no duplicate records)
        int updatedEvidenceCount = evidenceRepository.findByEventId(eventId).size();
        assertThat(updatedEvidenceCount).isEqualTo(initialEvidenceCount);
        log.info("Verified duplicate evidence protection: initialCount={}, afterSecondCallCount={}",
                initialEvidenceCount, updatedEvidenceCount);
    }

    @Test
    @Order(3)
    @DisplayName("P3-E2E: Invalid H3 format returns HTTP 400 Bad Request")
    void testInvalidH3Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/hotspot/short")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid H3 index")));
    }

    @Test
    @Order(4)
    @DisplayName("P3-E2E: Unknown H3 cell without hotspot returns HTTP 404 Not Found")
    void testUnknownH3Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/hotspot/886088500000000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("No active hotspot intelligence")));
    }
}
