package com.aerosentinel.evidence;

import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.hotspot.HotspotSpatialContext;
import com.aerosentinel.hotspot.HotspotSpatialContext.*;
import com.aerosentinel.dto.evidence.EvidenceSummaryResponse;
import com.aerosentinel.forecast.ForecastResponse;
import com.aerosentinel.forecast.ForecastResponse.ForecastItem;
import com.aerosentinel.evidence.EvidenceAiClient.EvidenceAiOutputDto;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import com.aerosentinel.forecast.ForecastService;
import com.aerosentinel.hotspot.HotspotContextService;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvidenceUnitTest {

    private HotspotContextService hotspotContextService;
    private ForecastService forecastService;
    private EvidenceAiClient evidenceAiClient;
    private GeminiAnalysisRepository geminiAnalysisRepository;
    private PollutionEventRepository pollutionEventRepository;
    private EvidenceRepository evidenceRepository;
    private GridService gridService;

    private EvidenceOrchestrationService orchestrationService;

    private static final String H3_INDEX = "88608850e5fffff";
    private static final String CITY_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final UUID PREDICTION_ID = UUID.fromString("a310c689-f340-49fc-8935-a037de8d7709");
    private static final UUID SNAPSHOT_ID = UUID.fromString("1624baa3-a5f8-407b-b1c2-36bcee7650b1");
    private static final UUID GRID_CELL_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        hotspotContextService = mock(HotspotContextService.class);
        forecastService = mock(ForecastService.class);
        evidenceAiClient = mock(EvidenceAiClient.class);
        geminiAnalysisRepository = mock(GeminiAnalysisRepository.class);
        pollutionEventRepository = mock(PollutionEventRepository.class);
        evidenceRepository = mock(EvidenceRepository.class);
        gridService = mock(GridService.class);

        orchestrationService = new EvidenceOrchestrationService(
                hotspotContextService,
                forecastService,
                evidenceAiClient,
                geminiAnalysisRepository,
                pollutionEventRepository,
                evidenceRepository,
                gridService
        );

        // Default mock behaviors for grid cell and event saving
        GridCell cell = new GridCell();
        cell.setId(GRID_CELL_ID);
        cell.setH3Index(H3_INDEX);
        when(gridService.getOrCreateGridCell(anyString(), any())).thenReturn(cell);

        when(pollutionEventRepository.save(any(PollutionEvent.class))).thenAnswer(inv -> {
            PollutionEvent p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        when(pollutionEventRepository.findByEventCode(anyString())).thenReturn(Optional.empty());
    }

    private HotspotSpatialContext createMockSpatialContext() {
        AirContext air = new AirContext("VALID", 78.0, 120.0, 37.0, 14.0, 0.9, 24.0, Instant.now(), "PUN-001", 78.0);
        WeatherContext weather = new WeatherContext("VALID", 30.1, 52.0, 11.9, 3.31, 271.0, 949.8, 0.0, Instant.now());
        MonitoringCoverageContext cov = new MonitoringCoverageContext("VALID", 0.27, 2, 0, 0.95);
        SpatialDispersionContext disp = new SpatialDispersionContext("VALID", 78.0, 4.3604, 0.0761);
        EnvironmentalGisContext gis = new EnvironmentalGisContext("VALID", 3.5, 0.4, 4, 0, 0, 0.0, 0.0, 50.0, 0.0, 0.0);
        ConfidenceBreakdown confBreakdown = new ConfidenceBreakdown(0.86, 0.90, 0.95, 0.80, 0.27, 0);

        return new HotspotSpatialContext(
                PREDICTION_ID,
                H3_INDEX,
                UUID.fromString(CITY_ID),
                "Pune",
                SNAPSHOT_ID,
                Instant.now(),
                0.80,
                "CRITICAL",
                0.86,
                "ML",
                "hotspot_classifier_v1",
                "LIVE",
                air,
                weather,
                cov,
                disp,
                gis,
                true,
                0.20,
                confBreakdown
        );
    }

    private ForecastResponse createMockForecastResponse(String h3) {
        Instant t0 = Instant.parse("2026-09-27T08:00:00Z");
        List<ForecastItem> horizons = List.of(
                new ForecastItem(1, t0.plusSeconds(3600), 71.90, 70.06, 73.76, "ug/m3"),
                new ForecastItem(3, t0.plusSeconds(10800), 70.43, 66.53, 73.48, "ug/m3"),
                new ForecastItem(6, t0.plusSeconds(21600), 70.55, 65.03, 75.98, "ug/m3")
        );
        return new ForecastResponse(
                h3,
                UUID.fromString(CITY_ID),
                t0,
                t0.plusSeconds(60),
                "forecast_regressors_v1",
                PREDICTION_ID,
                SNAPSHOT_ID,
                "SUCCESS",
                "LIVE",
                horizons,
                null // forecastConfidence strictly null
        );
    }

    private EvidenceAiOutputDto createMockAiOutputDto(String triageState, double score) {
        EvidenceAiOutputDto.ScoreBreakdownDto sb = new EvidenceAiOutputDto.ScoreBreakdownDto(
                0.35, 0.40, 0.33, 0.95, 0.67, 1.0, 0.0, 0.667, score
        );
        EvidenceAiOutputDto.AiInterpretationDto ai = new EvidenceAiOutputDto.AiInterpretationDto(
                "Elevated PM2.5 detected in area " + H3_INDEX,
                "Analyst diagnostic: High ground PM2.5 (78 ug/m3) corroborated by forecast",
                "High particulate elevation",
                List.of("PM2.5 concentration at 78.0 ug/m3", "Nearest monitor at 0.27 km"),
                "Trajectory stabilizes at ~71 ug/m3 across 6 hours",
                "Observations restricted to local CAAQMS station radius",
                List.of("Cannot confirm industrial stack emission cause without permit inspection"),
                false,
                true,
                "gemini-2.0-flash",
                "structured_event_explanation_v001"
        );
        EvidenceAiOutputDto.RecommendedVerificationDto rec = new EvidenceAiOutputDto.RecommendedVerificationDto(
                "Dispatch mobile CAAQMS rapid-sampling unit",
                "URGENT",
                List.of("Deploy mobile sensor to verify peak", "Inspect upwind emitters")
        );
        EvidenceAiOutputDto.ProvenanceDto prov = new EvidenceAiOutputDto.ProvenanceDto(
                H3_INDEX,
                CITY_ID,
                PREDICTION_ID.toString(),
                "hotspot_classifier_v1",
                "forecast_regressors_v1",
                "v1.0.0",
                "gemini-2.0-flash",
                "structured_event_explanation_v001",
                Instant.now().toString()
        );
        return new EvidenceAiOutputDto(
                "SUCCESS",
                H3_INDEX,
                CITY_ID,
                PREDICTION_ID.toString(),
                "EVT-88608850-2026092708-3f89a1c2",
                "EVT-88608850-2026092708-3f89a1c2",
                List.of(H3_INDEX),
                score,
                0.667,
                "consistent",
                triageState,
                sb,
                Map.of("ground_sensor", "supported", "satellite", "unavailable", "fire", "unavailable"),
                List.of("Satellite unavailable"),
                Collections.emptyList(),
                List.of("Ground PM2.5 measurement exceeds threshold"),
                List.of(new EvidenceAiOutputDto.SignalItemDto(
                        "sig-88608850-001", "DIRECT_OBSERVATION", "PM2.5 at 78 ug/m3",
                        Instant.now().toString(), 0.90, "CPCB", "PRIMARY", "PUN-001"
                )),
                ai,
                rec,
                prov
        );
    }

    @Test
    @DisplayName("1. Valid F3 + F4 -> EvidenceSummaryResponse strictly separates all 6 tiers")
    void testValidF3AndF4ProducesStructuredResponse() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("ALERT_CANDIDATE", 0.62));

        EvidenceSummaryResponse response = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");

        // Context
        assertThat(response.context().h3Index()).isEqualTo(H3_INDEX);
        assertThat(response.context().predictionId()).isEqualTo(PREDICTION_ID.toString());
        assertThat(response.context().eventId()).isNotEmpty();

        // Observed Facts
        assertThat(response.observedFacts().air().pm25()).isEqualTo(78.0);
        assertThat(response.observedFacts().weather().temperature()).isEqualTo(30.1);

        // Model Outputs
        assertThat(response.modelOutputs().hotspot().riskScore()).isEqualTo(0.80);
        assertThat(response.modelOutputs().hotspot().operationalThreshold()).isEqualTo(0.20);
        assertThat(response.modelOutputs().hotspot().isHotspot()).isTrue();
        assertThat(response.modelOutputs().forecast().forecastConfidence()).isNull();
        assertThat(response.modelOutputs().forecast().horizons()).hasSize(3);

        // Evidence & Triage
        assertThat(response.evidence().triageState()).isEqualTo("ALERT_CANDIDATE");
        assertThat(response.evidence().evidenceScore()).isEqualTo(0.62);

        // AI Interpretation
        assertThat(response.aiInterpretation().isGrounded()).isTrue();
        assertThat(response.aiInterpretation().causalClaimSupported()).isFalse();
        assertThat(response.aiInterpretation().summaryPublic()).contains(H3_INDEX);

        // Recommended Verification
        assertThat(response.recommendedVerification().priority()).isEqualTo("URGENT");
    }

    @Test
    @DisplayName("2. F3 Hotspot lineage is strictly preserved in Context and ModelOutputs")
    void testF3HotspotLineagePreserved() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("MONITOR", 0.45));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.context().predictionId()).isEqualTo(PREDICTION_ID.toString());
        assertThat(res.context().featureSnapshotId()).isEqualTo(SNAPSHOT_ID.toString());
        assertThat(res.modelOutputs().hotspot().modelVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(res.modelOutputs().hotspot().operationalThreshold()).isEqualTo(0.20);
    }

    @Test
    @DisplayName("3. F4 Forecast linkage is strictly preserved with null confidence")
    void testF4ForecastLinkagePreserved() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("MONITOR", 0.45));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.modelOutputs().forecast()).isNotNull();
        assertThat(res.modelOutputs().forecast().forecastModelVersion()).isEqualTo("forecast_regressors_v1");
        assertThat(res.modelOutputs().forecast().forecastConfidence()).isNull();
        assertThat(res.modelOutputs().forecast().parentPredictionId()).isEqualTo(PREDICTION_ID);
        assertThat(res.modelOutputs().forecast().horizons().get(0).horizonHours()).isEqualTo(1);
    }

    @Test
    @DisplayName("4. F5 scoring invocation passes F3 and F4 values to AI bridge")
    @SuppressWarnings("unchecked")
    void testF5ScoringBridgeInvocation() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("ALERT_CANDIDATE", 0.65));

        orchestrationService.getOrchestratedEvidence(H3_INDEX);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(evidenceAiClient, times(1)).evaluate(captor.capture());

        Map<String, Object> captured = captor.getValue();
        assertThat(captured.get("h3Index")).isEqualTo(H3_INDEX);
        assertThat(captured.get("predictionId")).isEqualTo(PREDICTION_ID.toString());
        assertThat(captured.get("hotspot")).isNotNull();
        assertThat(captured.get("forecast")).isNotNull();
    }

    @Test
    @DisplayName("5. Correct evidence score passthrough to response")
    void testEvidenceScorePassthrough() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("ALERT_CANDIDATE", 0.732));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.evidence().evidenceScore()).isEqualTo(0.732);
        assertThat(res.evidence().scoreBreakdown().finalEvidenceScore()).isEqualTo(0.732);
    }

    @Test
    @DisplayName("6. Correct triage state passthrough (INSUFFICIENT_EVIDENCE, MONITOR, ALERT_CANDIDATE)")
    void testTriageStatePassthrough() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("INSUFFICIENT_EVIDENCE", 0.22));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.evidence().triageState()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(res.evidence().evidenceScore()).isEqualTo(0.22);
    }

    @Test
    @DisplayName("7. Gemini structured output mapping preserves diagnostic fields")
    void testGeminiStructuredOutputMapping() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("MONITOR", 0.48));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.aiInterpretation().summaryPublic()).isNotEmpty();
        assertThat(res.aiInterpretation().summaryAnalyst()).isNotEmpty();
        assertThat(res.aiInterpretation().detectedCondition()).isEqualTo("High particulate elevation");
        assertThat(res.aiInterpretation().causalClaimSupported()).isFalse();
        assertThat(res.aiInterpretation().unsupportedConclusions()).hasSize(1);
    }

    @Test
    @DisplayName("8. Gemini analysis persistence links to PollutionEvent and HotspotPrediction")
    void testGeminiAnalysisPersistence() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("ALERT_CANDIDATE", 0.65));

        orchestrationService.getOrchestratedEvidence(H3_INDEX);

        ArgumentCaptor<GeminiAnalysis> captor = ArgumentCaptor.forClass(GeminiAnalysis.class);
        verify(geminiAnalysisRepository, times(1)).save(captor.capture());

        GeminiAnalysis saved = captor.getValue();
        assertThat(saved.getH3Index()).isEqualTo(H3_INDEX);
        assertThat(saved.getEventId()).isNotNull(); // Linked to PollutionEvent.id
        assertThat(saved.getPredictionId()).isEqualTo(PREDICTION_ID); // Preserves prediction lineage
        assertThat(saved.getDetectedCondition()).isEqualTo("High particulate elevation");
        assertThat(saved.getIsGrounded()).isTrue();
    }

    @Test
    @DisplayName("9. Missing forecast is gracefully tolerated without failing evidence synthesis")
    void testMissingForecastGracefulToleration() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty()); // No forecast
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("MONITOR", 0.38));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res).isNotNull();
        assertThat(res.status()).isEqualTo("SUCCESS");
        assertThat(res.modelOutputs().forecast()).isNull();
        assertThat(res.evidence().triageState()).isEqualTo("MONITOR");
    }

    @Test
    @DisplayName("10. Missing F3 context throws ResourceNotFoundException (no fabricated facts)")
    void testMissingF3ContextThrowsNotFound() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active hotspot intelligence");
    }

    @Test
    @DisplayName("11. Invalid H3 format throws ValidationException")
    void testInvalidH3ThrowsValidationException() {
        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence("invalid"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid H3 index");
    }

    @Test
    @DisplayName("12. AI bridge failure raises controlled exception without returning fake scores")
    void testAiBridgeFailureThrowsControlledException() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenThrow(new RuntimeException("CLI exit code 3"));

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evidence scoring bridge failed");
    }

    @Test
    @DisplayName("13. No duplicate forecast calculation: forecastService is queried, not regenerated")
    void testNoDuplicateForecastExecution() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("MONITOR", 0.40));

        orchestrationService.getOrchestratedEvidence(H3_INDEX);

        verify(forecastService, times(1)).getForecastByH3(H3_INDEX);
        verify(forecastService, never()).generateForecast(any());
    }

    @Test
    @DisplayName("14. Provenance fields correctly preserve f3, f4, f5, and gemini model versions")
    void testProvenanceVersionsCorrect() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("MONITOR", 0.40));

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.provenance().f3ModelVersion()).isEqualTo("hotspot_classifier_v1");
        assertThat(res.provenance().f4ModelVersion()).isEqualTo("forecast_regressors_v1");
        assertThat(res.provenance().f5ScoringVersion()).isEqualTo("v1.0.0");
        assertThat(res.provenance().geminiModelVersion()).isEqualTo("gemini-2.0-flash");
    }

    @Test
    @DisplayName("15. P3: F4 Forecast H3 mismatch is rejected with ValidationException")
    void testForecastH3MismatchRejection() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        // Return forecast for a different cell
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse("88608852c1fffff")));

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("H3 mismatch: requested " + H3_INDEX + " but F4 forecast is for 88608852c1fffff");

        verify(geminiAnalysisRepository, never()).save(any());
        verify(pollutionEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("16. P3: AI bridge H3 mismatch is rejected with ValidationException")
    void testAiBridgeH3MismatchRejection() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));

        // Output from AI with corrupted H3
        EvidenceAiOutputDto corruptOutput = new EvidenceAiOutputDto(
                "SUCCESS", "88608852c1fffff", CITY_ID, PREDICTION_ID.toString(),
                "EVT-88608852-2026092708-3f89a1c2", "EVT-88608852-2026092708-3f89a1c2",
                List.of("88608852c1fffff"), 0.65, 0.67, "consistent", "ALERT_CANDIDATE",
                null, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), null, null, null
        );
        when(evidenceAiClient.evaluate(any())).thenReturn(corruptOutput);

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("H3 mismatch: requested " + H3_INDEX + " but AI bridge returned 88608852c1fffff");

        verify(geminiAnalysisRepository, never()).save(any());
        verify(pollutionEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("17. P3: Deterministic Event creation preserves H3 and prediction lineage")
    void testPollutionEventCreationAndLineage() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("ALERT_CANDIDATE", 0.65));

        orchestrationService.getOrchestratedEvidence(H3_INDEX);

        ArgumentCaptor<PollutionEvent> eventCaptor = ArgumentCaptor.forClass(PollutionEvent.class);
        verify(pollutionEventRepository, times(1)).save(eventCaptor.capture());

        PollutionEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getH3Index()).isEqualTo(H3_INDEX);
        assertThat(savedEvent.getPredictionId()).isEqualTo(PREDICTION_ID);
        assertThat(savedEvent.getGridCellId()).isEqualTo(GRID_CELL_ID);
        assertThat(savedEvent.getEventCode()).isEqualTo("EVT-88608850-2026092708-3f89a1c2");
        assertThat(savedEvent.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("18. P3: Duplicate evidence protection avoids duplicate EventEvidence insertion")
    void testDuplicateEvidenceProtection() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutputDto("ALERT_CANDIDATE", 0.65));

        // Signal already exists in repository
        when(evidenceRepository.existsByEventIdAndEvidenceKey(any(), eq("sig-88608850-001"))).thenReturn(true);

        orchestrationService.getOrchestratedEvidence(H3_INDEX);

        // Should not save duplicate EventEvidence
        verify(evidenceRepository, never()).save(any(EventEvidence.class));
    }

    @Test
    @DisplayName("19. P3: Spatio-temporal event cluster cells are preserved in EvidenceDto")
    void testClusterCellsPreservedInEvidence() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.of(createMockForecastResponse(H3_INDEX)));

        List<String> cluster = List.of(H3_INDEX, "88608852c1fffff", "8860885357fffff");
        EvidenceAiOutputDto output = createMockAiOutputDto("ALERT_CANDIDATE", 0.65);
        // Replace cluster
        EvidenceAiOutputDto clusteredOutput = new EvidenceAiOutputDto(
                output.status(), output.h3Index(), output.cityId(), output.predictionId(),
                output.eventId(), output.canonicalEventId(), cluster,
                output.evidenceScore(), output.evidenceCompleteness(), output.consistency(),
                output.triageState(), output.scoreBreakdown(), output.sourceMatrix(),
                output.unavailableSources(), output.conflictingNotes(), output.supportingSignals(),
                output.signals(), output.aiInterpretation(), output.recommendedVerification(), output.provenance()
        );
        when(evidenceAiClient.evaluate(any())).thenReturn(clusteredOutput);

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res.evidence().clusterH3Cells()).containsExactlyElementsOf(cluster);
    }
}
