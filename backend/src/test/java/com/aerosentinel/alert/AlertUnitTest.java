package com.aerosentinel.alert;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.alert.AuthorityQueueItemDto;
import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.evidence.EvidenceAiClient.EvidenceAiOutputDto;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import com.aerosentinel.forecast.ForecastResponse;
import com.aerosentinel.forecast.ForecastResponse.ForecastItem;
import com.aerosentinel.hotspot.HotspotSpatialContext;
import com.aerosentinel.hotspot.HotspotSpatialContext.*;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertUnitTest {

    private AlertRepository alertRepository;
    private CityRepository cityRepository;
    private GeminiAnalysisRepository geminiAnalysisRepository;
    private AlertService alertService;

    private static final String H3_INDEX = "88608850e5fffff";
    private static final UUID EVENT_ID = UUID.fromString("9ea81bb7-9ff2-4b4d-a80f-32e98e2cb12f");
    private static final String EVENT_CODE = "EVT-88608850-2026092613-d75654e9";
    private static final UUID PREDICTION_ID = UUID.fromString("a310c689-f340-49fc-8935-a037de8d7709");
    private static final UUID CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID GRID_CELL_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        cityRepository = mock(CityRepository.class);
        geminiAnalysisRepository = mock(GeminiAnalysisRepository.class);

        alertService = new AlertService(alertRepository, cityRepository, geminiAnalysisRepository);

        City city = new City();
        city.setId(CITY_ID);
        city.setName("Pune");
        when(cityRepository.findById(CITY_ID)).thenReturn(Optional.of(city));
    }

    private PollutionEvent createMockEvent() {
        PollutionEvent event = new PollutionEvent();
        event.setId(EVENT_ID);
        event.setEventCode(EVENT_CODE);
        event.setH3Index(H3_INDEX);
        event.setPredictionId(PREDICTION_ID);
        event.setGridCellId(GRID_CELL_ID);
        event.setSeverity("CRITICAL");
        event.setStatus("OPEN");
        event.setCreatedAt(Instant.now());
        return event;
    }

    private HotspotSpatialContext createMockSpatialContext() {
        AirContext air = new AirContext("VALID", 78.0, 120.0, 37.0, 14.0, 0.9, 24.0, Instant.now(), "PUN-001", 78.0);
        WeatherContext weather = new WeatherContext("VALID", 21.8, 95.0, 4.9, 1.36, 246.0, 948.0, 0.0, Instant.now());
        MonitoringCoverageContext cov = new MonitoringCoverageContext("VALID", 0.27, 2, 0, 0.95);
        SpatialDispersionContext disp = new SpatialDispersionContext("VALID", 78.0, 4.3604, 0.0761);
        EnvironmentalGisContext gis = new EnvironmentalGisContext("VALID", 3.5, 0.4, 4, 0, 0, 0.0, 0.0, 50.0, 0.0, 0.0);
        ConfidenceBreakdown conf = new ConfidenceBreakdown(0.86, 0.90, 0.95, 0.80, 0.27, 0);

        return new HotspotSpatialContext(
                PREDICTION_ID,
                H3_INDEX,
                CITY_ID,
                "Pune",
                UUID.randomUUID(),
                Instant.now(),
                0.7998,
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

    private ForecastResponse createMockForecast() {
        Instant t0 = Instant.now();
        List<ForecastItem> horizons = List.of(
                new ForecastItem(1, t0.plusSeconds(3600), 70.62, 68.78, 72.48, "ug/m3"),
                new ForecastItem(3, t0.plusSeconds(10800), 70.55, 66.65, 73.60, "ug/m3"),
                new ForecastItem(6, t0.plusSeconds(21600), 60.91, 55.39, 66.33, "ug/m3")
        );
        return new ForecastResponse(
                H3_INDEX,
                CITY_ID,
                t0,
                t0.plusSeconds(60),
                "forecast_regressors_v1",
                PREDICTION_ID,
                UUID.randomUUID(),
                "SUCCESS",
                "LIVE",
                horizons,
                null
        );
    }

    private EvidenceAiOutputDto createMockAiOutput(String triageState, double score) {
        EvidenceAiOutputDto.ScoreBreakdownDto sb = new EvidenceAiOutputDto.ScoreBreakdownDto(
                0.35, 0.40, 0.33, 0.95, 0.67, 1.0, 0.0, score, score
        );
        EvidenceAiOutputDto.AiInterpretationDto ai = new EvidenceAiOutputDto.AiInterpretationDto(
                "Elevated PM2.5 detected in area " + H3_INDEX,
                "Analyst diagnostic: Ground PM2.5 (78 ug/m3) corroborated by forecast",
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
                CITY_ID.toString(),
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
                CITY_ID.toString(),
                PREDICTION_ID.toString(),
                EVENT_CODE,
                EVENT_CODE,
                List.of(H3_INDEX),
                score,
                0.667,
                "consistent",
                triageState,
                sb,
                Map.of("ground_sensor", "supported"),
                Collections.emptyList(),
                Collections.emptyList(),
                List.of("Ground PM2.5 measurement exceeds threshold"),
                Collections.emptyList(),
                ai,
                rec,
                prov
        );
    }

    @Test
    @DisplayName("1. ALERT_CANDIDATE triage state creates actionable alert candidate")
    void testAlertCandidateCreation() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.667);

        when(alertRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        Alert alert = alertOpt.get();
        assertThat(alert.getTriageState()).isEqualTo("ALERT_CANDIDATE");
        assertThat(alert.getStatus()).isEqualTo("OPEN");
        assertThat(alert.getEvidenceScore()).isEqualTo(0.667);
        assertThat(alert.getEventId()).isEqualTo(EVENT_ID);
        assertThat(alert.getEventCode()).isEqualTo(EVENT_CODE);
        assertThat(alert.getH3Index()).isEqualTo(H3_INDEX);
        assertThat(alert.getPredictionId()).isEqualTo(PREDICTION_ID);
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    @DisplayName("2. INSUFFICIENT_EVIDENCE does not create actionable alert")
    void testInsufficientEvidenceDoesNotCreateAlert() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("INSUFFICIENT_EVIDENCE", 0.157);

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isEmpty();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("3. MONITOR does not create actionable alert")
    void testMonitorDoesNotCreateAlert() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("MONITOR", 0.450);

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isEmpty();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("4. Existing event linkage preserved (eventId and eventCode)")
    void testEventLinkagePreserved() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.700);

        when(alertRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getEventId()).isEqualTo(EVENT_ID);
        assertThat(alertOpt.get().getEventCode()).isEqualTo(EVENT_CODE);
    }

    @Test
    @DisplayName("5. H3 lineage preserved")
    void testH3LineagePreserved() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.700);

        when(alertRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getH3Index()).isEqualTo(H3_INDEX);
    }

    @Test
    @DisplayName("6. Parent prediction lineage preserved")
    void testPredictionLineagePreserved() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.700);

        when(alertRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getPredictionId()).isEqualTo(PREDICTION_ID);
    }

    @Test
    @DisplayName("7. Evidence score copied from authoritative scoring result")
    void testEvidenceScoreCopiedAccurately() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.618);

        when(alertRepository.findByEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getEvidenceScore()).isEqualTo(0.618);
    }

    @Test
    @DisplayName("8. Duplicate alert protection: repeated call reuses existing alert")
    void testDuplicateAlertProtection() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.667);

        Alert existingAlert = new Alert();
        existingAlert.setId(UUID.randomUUID());
        existingAlert.setEventId(EVENT_ID);
        existingAlert.setEventCode(EVENT_CODE);
        existingAlert.setH3Index(H3_INDEX);
        existingAlert.setStatus("OPEN");

        when(alertRepository.findByEventId(EVENT_ID)).thenReturn(Optional.of(existingAlert));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getId()).isEqualTo(existingAlert.getId());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("9. Invalid status transition rejection: RESOLVED -> ACKNOWLEDGED or OPEN throws ValidationException")
    void testInvalidStatusTransitionRejection() {
        UUID alertId = UUID.randomUUID();
        Alert resolvedAlert = new Alert();
        resolvedAlert.setId(alertId);
        resolvedAlert.setStatus("RESOLVED");
        resolvedAlert.setResolvedAt(Instant.now());

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(resolvedAlert));

        assertThatThrownBy(() -> alertService.acknowledgeAlert(alertId, UUID.randomUUID()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot acknowledge an already resolved alert");
    }

    @Test
    @DisplayName("10. Valid acknowledgement transition: OPEN -> ACKNOWLEDGED")
    void testValidAcknowledgementTransition() {
        UUID alertId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Alert openAlert = new Alert();
        openAlert.setId(alertId);
        openAlert.setStatus("OPEN");

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(openAlert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert acked = alertService.acknowledgeAlert(alertId, userId);

        assertThat(acked.getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(acked.getAcknowledgedAt()).isNotNull();
        assertThat(acked.getAcknowledgedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("11. Valid resolution transition: OPEN or ACKNOWLEDGED -> RESOLVED")
    void testValidResolutionTransition() {
        UUID alertId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Alert ackedAlert = new Alert();
        ackedAlert.setId(alertId);
        ackedAlert.setStatus("ACKNOWLEDGED");

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(ackedAlert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        Alert resolved = alertService.resolveAlert(alertId, userId);

        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolvedBy()).isEqualTo(userId);
    }

    @Test
    @DisplayName("12. Missing event / null handling returns empty safely")
    void testMissingEventHandling() {
        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(null, null, null, null);
        assertThat(alertOpt).isEmpty();
    }

    @Test
    @DisplayName("13. Concurrent duplicate creation protection: DataIntegrityViolationException resolves to existing")
    void testConcurrentRaceSafety() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        EvidenceAiOutputDto ai = createMockAiOutput("ALERT_CANDIDATE", 0.667);

        Alert existingAlert = new Alert();
        existingAlert.setId(UUID.randomUUID());
        existingAlert.setEventId(EVENT_ID);

        // 1. Initial check sees empty (simulating race condition)
        when(alertRepository.findByEventId(EVENT_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAlert));

        // 2. Save throws DataIntegrityViolationException (another thread committed first)
        when(alertRepository.save(any(Alert.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isPresent();
        assertThat(alertOpt.get().getId()).isEqualTo(existingAlert.getId());
    }

    @Test
    @DisplayName("14. F7-P2: Attached Citizen Evidence is exposed in AuthorityQueueItemDto with safe URLs and auxiliary tier")
    void testCitizenEvidenceAttachedExposedInAuthorityQueueDto() {
        com.aerosentinel.evidence.EvidenceRepository mockEvidenceRepo = mock(com.aerosentinel.evidence.EvidenceRepository.class);
        com.aerosentinel.citizen.CitizenReportRepository mockCitizenRepo = mock(com.aerosentinel.citizen.CitizenReportRepository.class);

        AlertService alertServiceWithCitizen = new AlertService(
                alertRepository,
                cityRepository,
                geminiAnalysisRepository,
                null,
                null,
                mockEvidenceRepo,
                mockCitizenRepo
        );

        UUID citizenReportId = UUID.randomUUID();
        com.aerosentinel.evidence.EventEvidence citizenEv = new com.aerosentinel.evidence.EventEvidence();
        citizenEv.setId(UUID.randomUUID());
        citizenEv.setEventId(EVENT_ID);
        citizenEv.setDataSource("CITIZEN");
        citizenEv.setRelevanceTier("AUXILIARY");
        citizenEv.setSourceRef(citizenReportId.toString());
        citizenEv.setEvidenceKey("citizen-report-" + citizenReportId);
        citizenEv.setEvidenceValue("Citizen observation [SMOKE_LIKE]: Heavy plume near stacks");
        citizenEv.setConfidenceScore(0.95);
        citizenEv.setObservedAt(Instant.now());

        when(mockEvidenceRepo.findByEventId(EVENT_ID)).thenReturn(List.of(citizenEv));

        com.aerosentinel.citizen.CitizenReport report = new com.aerosentinel.citizen.CitizenReport();
        report.setId(citizenReportId);
        report.setH3Index(H3_INDEX);
        report.setCategory("INDUSTRIAL_EMISSION");
        report.setDescription("Heavy plume near stacks");
        report.setImageUrl("/api/v1/citizen/photos/" + UUID.randomUUID() + ".jpg");
        report.setSubmittedAt(citizenEv.getObservedAt());

        when(mockCitizenRepo.findById(citizenReportId)).thenReturn(Optional.of(report));

        GeminiAnalysis ga = new GeminiAnalysis();
        ga.setId(UUID.randomUUID());
        ga.setCitizenReportId(citizenReportId);
        ga.setDetectedCategory("SMOKE_LIKE");
        ga.setConfidence(0.95);
        ga.setNarrativeSummary("Dense smoke plumes rising from industrial chimney stack");
        ga.setUncertaintyStatement("Image alone cannot determine numerical pollutant concentration");

        when(geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(citizenReportId)).thenReturn(Optional.of(ga));

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setEventId(EVENT_ID);
        alert.setH3Index(H3_INDEX);
        alert.setStatus("OPEN");
        alert.setSeverity("CRITICAL");
        alert.setRiskScore(0.7998);
        alert.setEvidenceScore(0.667);
        alert.setTriageState("ALERT_CANDIDATE");
        alert.setTitle("Elevated Industrial Plume");
        alert.setMessage("Corroborated by sensor and citizen evidence");

        AuthorityQueueItemDto dto = alertServiceWithCitizen.toAuthorityQueueItemDto(alert);

        assertThat(dto.citizenEvidence()).isNotNull();
        assertThat(dto.citizenEvidence()).hasSize(1);

        var item = dto.citizenEvidence().get(0);
        assertThat(item.reportId()).isEqualTo(citizenReportId);
        assertThat(item.reportReference()).isEqualTo("CR-" + citizenReportId.toString().substring(0, 8).toUpperCase());
        assertThat(item.h3Index()).isEqualTo(H3_INDEX);
        assertThat(item.category()).isEqualTo("INDUSTRIAL_EMISSION");
        assertThat(item.visibleCondition()).isEqualTo("SMOKE_LIKE");
        assertThat(item.visualConfidence()).isEqualTo(0.95);
        assertThat(item.dataSource()).isEqualTo("CITIZEN");
        assertThat(item.relevanceTier()).isEqualTo("AUXILIARY");
        assertThat(item.photoUrl()).startsWith("/api/v1/citizen/photos/");
        assertThat(item.visualObservations()).contains("Dense smoke plumes rising from industrial chimney stack");
        assertThat(item.visualUncertainty()).contains("Image alone cannot determine numerical pollutant concentration");
    }

    @Test
    @DisplayName("15. F7-P2: Alert without citizen evidence returns null citizenEvidence safely")
    void testNoCitizenEvidenceAttachedReturnsNullCleanly() {
        com.aerosentinel.evidence.EvidenceRepository mockEvidenceRepo = mock(com.aerosentinel.evidence.EvidenceRepository.class);
        AlertService alertServiceWithCitizen = new AlertService(
                alertRepository,
                cityRepository,
                geminiAnalysisRepository,
                null,
                null,
                mockEvidenceRepo,
                null
        );

        when(mockEvidenceRepo.findByEventId(EVENT_ID)).thenReturn(Collections.emptyList());

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setEventId(EVENT_ID);
        alert.setH3Index(H3_INDEX);
        alert.setStatus("OPEN");
        alert.setSeverity("CRITICAL");
        alert.setRiskScore(0.7998);
        alert.setEvidenceScore(0.667);
        alert.setTriageState("ALERT_CANDIDATE");

        AuthorityQueueItemDto dto = alertServiceWithCitizen.toAuthorityQueueItemDto(alert);

        assertThat(dto.citizenEvidence()).isNull();
        assertThat(dto.alertId()).isEqualTo(alert.getId());
    }

    @Test
    @DisplayName("16. F7-P2: Citizen evidence alone cannot bypass F5 alert gating (INSUFFICIENT_EVIDENCE blocks alert creation)")
    void testCitizenEvidenceAloneCannotBypassF5Triage() {
        PollutionEvent event = createMockEvent();
        HotspotSpatialContext ctx = createMockSpatialContext();
        ForecastResponse fc = createMockForecast();
        // Even if citizen visual confidence is high, if F5 triage is INSUFFICIENT_EVIDENCE, alert must NOT be created
        EvidenceAiOutputDto ai = createMockAiOutput("INSUFFICIENT_EVIDENCE", 0.224);

        Optional<Alert> alertOpt = alertService.createOrUpdateAlertCandidate(event, ctx, fc, ai);

        assertThat(alertOpt).isEmpty();
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    @DisplayName("17. F7-P5: Complete Alert Detail DTO preserves all connected lineage and operational fields")
    void testF7P5CompleteAlertDetailContextIntegrity() {
        Alert alert = new Alert();
        UUID alertId = UUID.randomUUID();
        alert.setId(alertId);
        alert.setEventId(EVENT_ID);
        alert.setEventCode(EVENT_CODE);
        alert.setH3Index(H3_INDEX);
        alert.setPredictionId(PREDICTION_ID);
        alert.setCityId(CITY_ID);
        alert.setStatus("OPEN");
        alert.setSeverity("CRITICAL");
        alert.setRiskScore(0.7998);
        alert.setEvidenceScore(0.2240);
        alert.setTriageState("ALERT_CANDIDATE");
        alert.setConsistency("CONSISTENT");
        alert.setTitle("Severe Particulate Elevation near Shivaji Nagar");
        alert.setMessage("Telemetry corroborated with ground sensor observation.");
        alert.setForecastSummary("+1h: 70.6 ug/m3 | +3h: 70.5 ug/m3 | +6h: 60.9 ug/m3");
        alert.setRecommendedAction("Deploy inspection team to Shivaji Nagar rail perimeter");
        alert.setCreatedAt(Instant.now());

        AuthorityQueueItemDto dto = alertService.toAuthorityQueueItemDto(alert);

        assertThat(dto.alertId()).isEqualTo(alertId);
        assertThat(dto.eventId()).isEqualTo(EVENT_ID);
        assertThat(dto.eventCode()).isEqualTo(EVENT_CODE);
        assertThat(dto.h3Index()).isEqualTo(H3_INDEX);
        assertThat(dto.parentPredictionId()).isEqualTo(PREDICTION_ID);
        assertThat(dto.cityName()).isEqualTo("Pune");
        assertThat(dto.status()).isEqualTo("OPEN");
        assertThat(dto.severity()).isEqualTo("CRITICAL");
        assertThat(dto.riskScore()).isEqualTo(0.7998);
        assertThat(dto.evidenceScore()).isEqualTo(0.2240);
        assertThat(dto.forecastSummary()).contains("+1h");
        assertThat(dto.recommendedAction()).contains("Deploy inspection team");
    }

    @Test
    @DisplayName("18. F7-P5: Metric Separation - F3 riskScore (0.7998) != F5 evidenceScore (0.2240)")
    void testF7P5MetricSeparationIntegrity() {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setRiskScore(0.7998);
        alert.setEvidenceScore(0.2240);

        AuthorityQueueItemDto dto = alertService.toAuthorityQueueItemDto(alert);

        assertThat(dto.riskScore()).isEqualTo(0.7998);
        assertThat(dto.evidenceScore()).isEqualTo(0.2240);
        assertThat(dto.riskScore()).isNotEqualTo(dto.evidenceScore());
    }
}

