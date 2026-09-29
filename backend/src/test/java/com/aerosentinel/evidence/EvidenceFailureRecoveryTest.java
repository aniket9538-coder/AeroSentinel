package com.aerosentinel.evidence;

import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.alert.AlertService;
import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.evidence.EvidenceSummaryResponse;
import com.aerosentinel.dto.inspection.AssignTeamRequest;
import com.aerosentinel.dto.inspection.SubmitVerificationRequest;
import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.evidence.EvidenceAiClient.EvidenceAiOutputDto;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.forecast.ForecastResponse;
import com.aerosentinel.forecast.ForecastService;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.hotspot.HotspotContextService;
import com.aerosentinel.hotspot.HotspotSpatialContext;
import com.aerosentinel.hotspot.HotspotSpatialContext.*;
import com.aerosentinel.inspection.*;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("F5-P7: Failure Recovery & Operational Hardening Tests")
public class EvidenceFailureRecoveryTest {

    private HotspotContextService hotspotContextService;
    private ForecastService forecastService;
    private EvidenceAiClient evidenceAiClient;
    private GeminiAnalysisRepository geminiAnalysisRepository;
    private PollutionEventRepository pollutionEventRepository;
    private EvidenceRepository evidenceRepository;
    private GridService gridService;
    private AlertService alertService;
    private AlertRepository alertRepository;
    private CityRepository cityRepository;

    private InspectionRepository inspectionRepository;
    private FieldTeamRepository fieldTeamRepository;
    private FieldVerificationRepository fieldVerificationRepository;
    private InspectionService inspectionService;

    private EvidenceOrchestrationService orchestrationService;

    private static final String H3_INDEX = "88608850e5fffff";
    private static final UUID PREDICTION_ID = UUID.fromString("a310c689-f340-49fc-8935-a037de8d7709");
    private static final UUID SNAPSHOT_ID = UUID.fromString("1624baa3-a5f8-407b-b1c2-36bcee7650b1");
    private static final UUID CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID GRID_CELL_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final UUID EVENT_ID = UUID.fromString("446928c2-6090-437d-a7c7-a9909e648403");
    private static final UUID ALERT_ID = UUID.fromString("e12da246-dbea-4028-aa51-bb667fcca393");
    private static final UUID TEAM_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440001");
    private static final UUID INSPECTION_ID = UUID.fromString("880e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        hotspotContextService = mock(HotspotContextService.class);
        forecastService = mock(ForecastService.class);
        evidenceAiClient = mock(EvidenceAiClient.class);
        geminiAnalysisRepository = mock(GeminiAnalysisRepository.class);
        pollutionEventRepository = mock(PollutionEventRepository.class);
        evidenceRepository = mock(EvidenceRepository.class);
        gridService = mock(GridService.class);
        alertRepository = mock(AlertRepository.class);
        cityRepository = mock(CityRepository.class);

        inspectionRepository = mock(InspectionRepository.class);
        fieldTeamRepository = mock(FieldTeamRepository.class);
        fieldVerificationRepository = mock(FieldVerificationRepository.class);

        alertService = new AlertService(alertRepository, cityRepository, geminiAnalysisRepository,
                inspectionRepository, fieldVerificationRepository);

        inspectionService = new InspectionService(inspectionRepository, fieldTeamRepository,
                fieldVerificationRepository, alertRepository);

        orchestrationService = new EvidenceOrchestrationService(
                hotspotContextService,
                forecastService,
                evidenceAiClient,
                geminiAnalysisRepository,
                pollutionEventRepository,
                evidenceRepository,
                gridService,
                alertService
        );

        GridCell cell = new GridCell();
        cell.setId(GRID_CELL_ID);
        cell.setH3Index(H3_INDEX);
        when(gridService.getOrCreateGridCell(anyString(), any())).thenReturn(cell);

        City city = new City();
        city.setId(CITY_ID);
        city.setName("Pune");
        when(cityRepository.findById(CITY_ID)).thenReturn(Optional.of(city));

        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            return a;
        });
    }

    private HotspotSpatialContext createMockSpatialContext() {
        AirContext air = new AirContext("VALID", 78.0, 120.0, 37.0, 14.0, 0.9, 24.0, Instant.now(), "PUN-001", 78.0);
        WeatherContext weather = new WeatherContext("VALID", 30.1, 52.0, 11.9, 3.31, 271.0, 949.8, 0.0, Instant.now());
        MonitoringCoverageContext cov = new MonitoringCoverageContext("VALID", 0.27, 2, 0, 0.95);
        SpatialDispersionContext disp = new SpatialDispersionContext("VALID", 78.0, 4.3604, 0.0761);
        EnvironmentalGisContext gis = new EnvironmentalGisContext("VALID", 3.5, 0.4, 4, 0, 0, 0.0, 0.0, 50.0, 0.0, 0.0);
        ConfidenceBreakdown conf = new ConfidenceBreakdown(0.86, 0.90, 0.95, 0.80, 0.27, 0);

        return new HotspotSpatialContext(
                PREDICTION_ID,
                H3_INDEX,
                CITY_ID,
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
                conf
        );
    }

    private EvidenceAiOutputDto createMockAiOutput(String triageState, double score) {
        EvidenceAiOutputDto.ScoreBreakdownDto sb = new EvidenceAiOutputDto.ScoreBreakdownDto(
                0.35, 0.40, 0.33, 0.95, 0.67, 1.0, 0.0, 0.667, score
        );
        EvidenceAiOutputDto.AiInterpretationDto ai = new EvidenceAiOutputDto.AiInterpretationDto(
                "Elevated PM2.5 detected in area " + H3_INDEX,
                "Analyst diagnostic: High ground PM2.5 corroborated by model",
                "High particulate elevation",
                List.of("PM2.5 concentration at 78.0 ug/m3"),
                "Trajectory stabilizes",
                "Bounded uncertainty",
                List.of("Cannot confirm legal causation"),
                false,
                true,
                "deterministic-fallback-v1.0",
                "structured_event_explanation_v001"
        );
        EvidenceAiOutputDto.RecommendedVerificationDto rec = new EvidenceAiOutputDto.RecommendedVerificationDto(
                "Dispatch mobile unit", "URGENT", List.of("Inspect stack")
        );
        EvidenceAiOutputDto.ProvenanceDto prov = new EvidenceAiOutputDto.ProvenanceDto(
                H3_INDEX, CITY_ID.toString(), PREDICTION_ID.toString(),
                "hotspot_classifier_v1", "unavailable", "v1.0.0",
                "deterministic-fallback-v1.0", "structured_event_explanation_v001",
                Instant.now().toString()
        );
        return new EvidenceAiOutputDto(
                "SUCCESS", H3_INDEX, CITY_ID.toString(), PREDICTION_ID.toString(),
                "EVT-88608850-2026092812", "EVT-88608850-2026092812",
                List.of(H3_INDEX), score, 0.667, "consistent", triageState,
                sb, Map.of("ground_sensor", "supported"), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                ai, rec, prov
        );
    }

    // =========================================================================
    // FAILURE INJECTION TESTS 1 - 10
    // =========================================================================

    @Test
    @DisplayName("P7-1: Gemini timeout - handled without crash and raises controlled exception")
    void test1_geminiTimeout_handledWithoutFabrication() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty());
        when(evidenceAiClient.evaluate(any())).thenThrow(new RuntimeException("Evidence inference timed out after 15000 ms"));

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evidence scoring bridge failed");

        // Verify downstream records are never created
        verify(pollutionEventRepository, never()).save(any());
        verify(geminiAnalysisRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("P7-2: Gemini malformed response - schema validation rejects and aborts persistence")
    void test2_geminiMalformedResponse_rejected() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty());
        when(evidenceAiClient.evaluate(any())).thenThrow(new RuntimeException("Failed to parse evidence CLI output: Unexpected token"));

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Evidence scoring bridge failed");

        verify(pollutionEventRepository, never()).save(any());
        verify(geminiAnalysisRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("P7-3: F4 unavailable - truthfully preserved as null, F3 and F5 scoring intact")
    void test3_f4ForecastUnavailable_preservesObservedAndF3DataTruthfully() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty()); // F4 unavailable
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutput("ALERT_CANDIDATE", 0.65));

        PollutionEvent event = new PollutionEvent();
        event.setId(EVENT_ID);
        event.setEventCode("EVT-88608850-2026092812");
        when(pollutionEventRepository.findByEventCode(any())).thenReturn(Optional.of(event));
        when(pollutionEventRepository.save(any())).thenReturn(event);

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);

        assertThat(res).isNotNull();
        assertThat(res.status()).isEqualTo("SUCCESS");
        // F4 must be strictly null (not synthetic zeros)
        assertThat(res.modelOutputs().forecast()).isNull();
        // F3 must be preserved
        assertThat(res.modelOutputs().hotspot().riskScore()).isEqualTo(0.80);
        assertThat(res.modelOutputs().hotspot().isHotspot()).isTrue();
        // Evidence scoring must remain authoritative
        assertThat(res.evidence().evidenceScore()).isEqualTo(0.65);
    }

    @Test
    @DisplayName("P7-4: F3 unavailable - returns 404 NOT_FOUND without fabricating risk score")
    void test4_f3Unavailable_throwsResourceNotFoundWithoutFabrication() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active hotspot intelligence");

        verify(evidenceAiClient, never()).evaluate(any());
        verify(pollutionEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("P7-5: Database persistence failure - triggers transaction abort without partial state")
    void test5_databasePersistenceFailure_triggersRollback() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty());
        when(evidenceAiClient.evaluate(any())).thenReturn(createMockAiOutput("ALERT_CANDIDATE", 0.65));

        // Simulate database persistence failure on PollutionEvent save
        when(pollutionEventRepository.findByEventCode(any())).thenReturn(Optional.empty());
        when(pollutionEventRepository.save(any())).thenThrow(new RuntimeException("PostgreSQL connection timeout (500)"));

        assertThatThrownBy(() -> orchestrationService.getOrchestratedEvidence(H3_INDEX))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PostgreSQL connection timeout");

        verify(geminiAnalysisRepository, never()).save(any());
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("P7-6: Alert creation conflict - race caught by unique constraint reuses existing canonical alert")
    void test6_alertCreationConflict_reusesExistingAlert() {
        PollutionEvent event = new PollutionEvent();
        event.setId(EVENT_ID);
        event.setEventCode("EVT-88608850-2026092812");
        HotspotSpatialContext ctx = createMockSpatialContext();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.667);

        Alert existingAlert = new Alert();
        existingAlert.setId(ALERT_ID);
        existingAlert.setEventId(EVENT_ID);

        // 1. Initial lookup sees empty (concurrent race condition)
        when(alertRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAlert));

        // 2. Save fails with unique constraint violation
        when(alertRepository.save(any(Alert.class)))
                .thenThrow(new DataIntegrityViolationException("unique_event_id_violation"));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, null, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getId()).isEqualTo(ALERT_ID);
    }

    @Test
    @DisplayName("P7-7: Duplicate retry protection - repeated orchestration reuses existing event and signals")
    void test7_duplicateOrchestrationRetry_isIdempotent() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty());

        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.65);
        when(evidenceAiClient.evaluate(any())).thenReturn(ai);

        PollutionEvent existingEvent = new PollutionEvent();
        existingEvent.setId(EVENT_ID);
        existingEvent.setEventCode("EVT-88608850-2026092812");

        when(pollutionEventRepository.findByEventCode("EVT-88608850-2026092812")).thenReturn(Optional.of(existingEvent));
        when(pollutionEventRepository.save(any())).thenReturn(existingEvent);

        // Simulate that signals are already saved
        when(evidenceRepository.existsByEventIdAndEvidenceKey(any(), any())).thenReturn(true);

        orchestrationService.getOrchestratedEvidence(H3_INDEX);

        // Does not create a second new event
        verify(gridService, never()).getOrCreateGridCell(any(), any());
        verify(evidenceRepository, never()).save(any(EventEvidence.class));
    }

    @Test
    @DisplayName("P7-8: Concurrent orchestration race - database duplicate prevention preserves single canonical event")
    void test8_concurrentOrchestrationRace_handledSafely() {
        when(hotspotContextService.buildSpatialContextForH3(H3_INDEX)).thenReturn(Optional.of(createMockSpatialContext()));
        when(forecastService.getForecastByH3(H3_INDEX)).thenReturn(Optional.empty());

        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.65);
        when(evidenceAiClient.evaluate(any())).thenReturn(ai);

        PollutionEvent existingEvent = new PollutionEvent();
        existingEvent.setId(EVENT_ID);
        existingEvent.setEventCode("EVT-88608850-2026092812");

        when(pollutionEventRepository.findByEventCode(any())).thenReturn(Optional.of(existingEvent));
        when(pollutionEventRepository.save(any())).thenReturn(existingEvent);

        EvidenceSummaryResponse res = orchestrationService.getOrchestratedEvidence(H3_INDEX);
        assertThat(res.context().eventId()).isEqualTo(EVENT_ID.toString());
    }

    @Test
    @DisplayName("P7-9: Invalid field workflow transitions - strictly rejected with state machine errors")
    void test9_invalidFieldWorkflowTransitions_rejected() {
        Alert resolvedAlert = new Alert();
        resolvedAlert.setId(ALERT_ID);
        resolvedAlert.setStatus("RESOLVED");
        when(alertRepository.findById(ALERT_ID)).thenReturn(Optional.of(resolvedAlert));

        AssignTeamRequest assignReq = new AssignTeamRequest(TEAM_ID, null, null, null);

        // A. Assignment to RESOLVED alert is rejected
        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, assignReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign field team to a RESOLVED alert");

        // B. Assignment to OPEN (unacknowledged) alert is rejected
        resolvedAlert.setStatus("OPEN");
        assertThatThrownBy(() -> inspectionService.assignFieldTeam(ALERT_ID, assignReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Alert must be ACKNOWLEDGED before assigning a field team");

        // C. Start inspection on COMPLETED inspection is rejected
        Inspection completedInspection = new Inspection();
        completedInspection.setId(INSPECTION_ID);
        completedInspection.setStatus("COMPLETED");
        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(completedInspection));

        assertThatThrownBy(() -> inspectionService.startInspection(INSPECTION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot start inspection in status: COMPLETED");

        // D. Submit verification on non-IN_PROGRESS inspection is rejected
        completedInspection.setStatus("ASSIGNED");
        SubmitVerificationRequest verReq = new SubmitVerificationRequest("CONFIRMED", "Smoke observed", null, null, null, null);

        assertThatThrownBy(() -> inspectionService.submitVerification(INSPECTION_ID, verReq))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inspection must be IN_PROGRESS to submit verification");

        // E. Submit verification with invalid result string is rejected
        completedInspection.setStatus("IN_PROGRESS");
        SubmitVerificationRequest invalidVerReq = new SubmitVerificationRequest("UNKNOWN_RESULT", "Smoke observed", null, null, null, null);

        assertThatThrownBy(() -> inspectionService.submitVerification(INSPECTION_ID, invalidVerReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid verification result");
    }

    @Test
    @DisplayName("P7-10: Verification persistence failure - aborts transaction and preserves inspection integrity")
    void test10_verificationPersistenceFailure_abortsTransaction() {
        Inspection inspection = new Inspection();
        inspection.setId(INSPECTION_ID);
        inspection.setStatus("IN_PROGRESS");
        when(inspectionRepository.findById(INSPECTION_ID)).thenReturn(Optional.of(inspection));

        // Simulate database exception during FieldVerification save
        when(fieldVerificationRepository.save(any(FieldVerification.class)))
                .thenThrow(new RuntimeException("Database error during verification write"));

        SubmitVerificationRequest req = new SubmitVerificationRequest("CONFIRMED", "Heavy smoke plume", null, null, null, null);

        assertThatThrownBy(() -> inspectionService.submitVerification(INSPECTION_ID, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error during verification write");

        // Inspection must NOT be completed if verification persistence fails
        assertThat(inspection.getStatus()).isEqualTo("IN_PROGRESS");
    }
}
