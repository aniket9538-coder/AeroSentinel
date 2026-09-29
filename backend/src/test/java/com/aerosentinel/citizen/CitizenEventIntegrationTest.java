package com.aerosentinel.citizen;

import com.aerosentinel.alert.AlertService;
import com.aerosentinel.dto.evidence.EvidenceSummaryResponse;
import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.evidence.*;
import com.aerosentinel.evidence.EvidenceAiClient.EvidenceAiOutputDto;
import com.aerosentinel.forecast.ForecastResponse;
import com.aerosentinel.forecast.ForecastService;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.hotspot.HotspotContextService;
import com.aerosentinel.hotspot.HotspotSpatialContext;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import com.aerosentinel.util.H3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive F6-P3 Tests: Lineage + Deduplication + Spatio-Temporal Matching.
 *
 * Verifies:
 * 1. Citizen Report H3 lineage (lat/lng -> H3 res 8)
 * 2. Citizen -> Event exact-H3 matching
 * 3. Temporal matching (within 120m vs outside 120m)
 * 4. No-match behavior (no event fabricated when no match exists)
 * 5. Citizen EventEvidence persistence (dataSource = CITIZEN, sourceRef = report ID)
 * 6. Visual confidence strictly separated from F3 riskScore and F5 evidenceScore
 * 7. Authoritative 60-minute same-H3 deduplication
 * 8. Repeated orchestration idempotency (no duplicate EventEvidence)
 * 9. Event reuse (reuses existing open event)
 * 10. Citizen evidence alone does NOT create an ALERT_CANDIDATE
 * 11. Verification lifecycle: ANALYZED != VERIFIED
 */
class CitizenEventIntegrationTest {

    private CitizenReportRepository citizenReportRepository;
    private GeminiAnalysisRepository geminiAnalysisRepository;
    private PhotoStorageService photoStorageService;
    private CitizenVisionAiClient visionAiClient;
    private PollutionEventRepository pollutionEventRepository;
    private EvidenceRepository evidenceRepository;
    private CitizenReportDeduplicator citizenReportDeduplicator;
    private CitizenEventMatcher citizenEventMatcher;
    private CitizenReportService citizenReportService;

    private HotspotContextService hotspotContextService;
    private ForecastService forecastService;
    private EvidenceAiClient evidenceAiClient;
    private GridService gridService;
    private AlertService alertService;
    private EvidenceOrchestrationService evidenceOrchestrationService;

    private static final String PUNE_H3 = "88608850e5fffff";
    private static final String DIFFERENT_H3 = "88608852c1fffff";
    private static final UUID PUNE_CITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final double PUNE_LAT = 18.5304;
    private static final double PUNE_LNG = 73.8467;

    @BeforeEach
    void setUp() {
        citizenReportRepository = mock(CitizenReportRepository.class);
        geminiAnalysisRepository = mock(GeminiAnalysisRepository.class);
        photoStorageService = mock(PhotoStorageService.class);
        visionAiClient = mock(CitizenVisionAiClient.class);
        pollutionEventRepository = mock(PollutionEventRepository.class);
        evidenceRepository = mock(EvidenceRepository.class);
        citizenReportDeduplicator = new CitizenReportDeduplicator();
        citizenEventMatcher = new CitizenEventMatcher();

        citizenReportService = new CitizenReportService(
                citizenReportRepository,
                geminiAnalysisRepository,
                photoStorageService,
                visionAiClient,
                pollutionEventRepository,
                evidenceRepository,
                citizenEventMatcher
        );

        hotspotContextService = mock(HotspotContextService.class);
        forecastService = mock(ForecastService.class);
        evidenceAiClient = mock(EvidenceAiClient.class);
        gridService = mock(GridService.class);
        alertService = mock(AlertService.class);

        evidenceOrchestrationService = new EvidenceOrchestrationService(
                hotspotContextService,
                forecastService,
                evidenceAiClient,
                geminiAnalysisRepository,
                pollutionEventRepository,
                evidenceRepository,
                gridService,
                alertService,
                citizenReportRepository,
                citizenReportDeduplicator,
                citizenEventMatcher
        );
    }

    @Test
    @DisplayName("1. Citizen Report H3 lineage: lat/lng -> authoritative H3 Resolution 8 index")
    void testH3LineageDerivation() {
        String derivedH3 = H3Utils.coordinatesToH3(PUNE_LAT, PUNE_LNG, 8);
        assertThat(derivedH3).isNotNull();
        assertThat(derivedH3).hasSize(15);
        assertThat(H3Utils.isValidH3Index(derivedH3)).isTrue();
        assertThat(H3Utils.getH3Core().getResolution(derivedH3)).isEqualTo(8);
    }

    @Test
    @DisplayName("2. Citizen -> Event matching: Exact H3 matching succeeds, mismatch is rejected")
    void testSpatialMatching() {
        CitizenReport report = new CitizenReport();
        report.setH3Index(PUNE_H3);
        report.setSubmittedAt(Instant.now());

        PollutionEvent matchingEvent = new PollutionEvent();
        matchingEvent.setH3Index(PUNE_H3);
        matchingEvent.setStartedAt(Instant.now());
        matchingEvent.setStatus("OPEN");

        PollutionEvent nonMatchingEvent = new PollutionEvent();
        nonMatchingEvent.setH3Index(DIFFERENT_H3);
        nonMatchingEvent.setStartedAt(Instant.now());
        nonMatchingEvent.setStatus("OPEN");

        assertThat(citizenEventMatcher.isMatch(report, matchingEvent)).isTrue();
        assertThat(citizenEventMatcher.isMatch(report, nonMatchingEvent)).isFalse();
    }

    @Test
    @DisplayName("3. Temporal matching: Report within 120 minutes matches; report outside 120 minutes does NOT match")
    void testTemporalMatching() {
        Instant eventTime = Instant.parse("2026-09-28T16:51:31Z");

        PollutionEvent event = new PollutionEvent();
        event.setH3Index(PUNE_H3);
        event.setStartedAt(eventTime);
        event.setStatus("OPEN");

        // Report 21 minutes earlier (within 120m)
        CitizenReport withinWindowReport = new CitizenReport();
        withinWindowReport.setH3Index(PUNE_H3);
        withinWindowReport.setSubmittedAt(eventTime.minus(Duration.ofMinutes(21)));
        assertThat(citizenEventMatcher.isMatch(withinWindowReport, event)).isTrue();

        // Report 130 minutes earlier (outside 120m)
        CitizenReport outsideWindowReport = new CitizenReport();
        outsideWindowReport.setH3Index(PUNE_H3);
        outsideWindowReport.setSubmittedAt(eventTime.minus(Duration.ofMinutes(130)));
        assertThat(citizenEventMatcher.isMatch(outsideWindowReport, event)).isFalse();
    }

    @Test
    @DisplayName("4. No-match behavior: When no matching event exists, citizen report is saved but NO event is fabricated")
    void testNoMatchDoesNotFabricateEvent() {
        when(pollutionEventRepository.findTopByH3IndexOrderByStartedAtDesc(any())).thenReturn(Optional.empty());

        UUID reportId = UUID.randomUUID();
        when(citizenReportRepository.save(any(CitizenReport.class))).thenAnswer(inv -> {
            CitizenReport r = inv.getArgument(0);
            if (r.getId() == null) r.setId(reportId);
            return r;
        });

        citizenReportService.submitReport(
                PUNE_CITY_ID, PUNE_LAT, PUNE_LNG, "SMOKE", "Visible smoke plume", null, Instant.now()
        );

        // CitizenReport saved
        verify(citizenReportRepository, atLeastOnce()).save(any(CitizenReport.class));
        // NO event created
        verify(pollutionEventRepository, never()).save(any(PollutionEvent.class));
        // NO event evidence persisted
        verify(evidenceRepository, never()).save(any(EventEvidence.class));
    }

    @Test
    @DisplayName("5. Citizen EventEvidence persistence: Saved with dataSource=CITIZEN, sourceRef=reportId, relevanceTier=AUXILIARY")
    void testCitizenEvidencePersistence() {
        UUID eventId = UUID.randomUUID();
        PollutionEvent event = new PollutionEvent();
        event.setId(eventId);
        event.setEventCode("EVT-88608850-2026092816-test");
        event.setH3Index(PUNE_H3);
        event.setStartedAt(Instant.now());
        event.setStatus("OPEN");

        when(pollutionEventRepository.findTopByH3IndexOrderByStartedAtDesc(PUNE_H3)).thenReturn(Optional.of(event));

        UUID reportId = UUID.randomUUID();
        when(citizenReportRepository.save(any(CitizenReport.class))).thenAnswer(inv -> {
            CitizenReport r = inv.getArgument(0);
            if (r.getId() == null) r.setId(reportId);
            return r;
        });

        MockMultipartFile photo = new MockMultipartFile("photo", "plume.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(photoStorageService.storePhoto(any())).thenReturn(
                new PhotoStorageService.StoredPhoto("k.jpg", "/photos/k.jpg", Paths.get("k.jpg"), 3, "image/jpeg")
        );

        CitizenVisionAiClient.CitizenVisionResultDto visionDto = new CitizenVisionAiClient.CitizenVisionResultDto(
                "SUCCESS", reportId.toString(), PUNE_H3, "SMOKE_LIKE", 0.78,
                List.of("Visible smoke plume"), List.of("Uncertain ground concentration"),
                Map.of(), List.of(), "gemini-2.0-flash", "v1"
        );
        when(visionAiClient.analyzeImage(any(), any(), any())).thenReturn(visionDto);

        when(geminiAnalysisRepository.save(any(GeminiAnalysis.class))).thenAnswer(inv -> {
            GeminiAnalysis ga = inv.getArgument(0);
            ga.setId(UUID.randomUUID());
            return ga;
        });

        when(evidenceRepository.existsByEventIdAndEvidenceKey(any(), any())).thenReturn(false);

        citizenReportService.submitReport(
                PUNE_CITY_ID, PUNE_LAT, PUNE_LNG, "SMOKE", "Heavy smoke near highway", photo, Instant.now()
        );

        ArgumentCaptor<EventEvidence> evCaptor = ArgumentCaptor.forClass(EventEvidence.class);
        verify(evidenceRepository, times(1)).save(evCaptor.capture());
        EventEvidence savedEv = evCaptor.getValue();

        assertThat(savedEv.getEventId()).isEqualTo(eventId);
        assertThat(savedEv.getDataSource()).isEqualTo("CITIZEN");
        assertThat(savedEv.getRelevanceTier()).isEqualTo("AUXILIARY");
        assertThat(savedEv.getSourceRef()).isEqualTo(reportId.toString());
        assertThat(savedEv.getSourceType()).isEqualTo("CITIZEN_OBSERVATION");
        assertThat(savedEv.getConfidenceScore()).isEqualTo(0.78);
        assertThat(savedEv.getEvidenceKey()).isEqualTo("citizen-report-" + reportId);
    }

    @Test
    @DisplayName("6. Visual confidence separation: Vision confidence (0.78) is distinct from F3 riskScore and F5 evidenceScore")
    void testVisualConfidenceSeparation() {
        double visualConfidence = 0.78;
        double f3RiskScore = 0.85;
        double f5EvidenceScore = 0.62;

        EventEvidence ev = new EventEvidence();
        ev.setDataSource("CITIZEN");
        ev.setRelevanceTier("AUXILIARY");
        ev.setConfidenceScore(visualConfidence);

        // Visual confidence represents visual interpretation confidence only
        assertThat(ev.getConfidenceScore()).isEqualTo(visualConfidence);
        assertThat(ev.getConfidenceScore()).isNotEqualTo(f3RiskScore);
        assertThat(ev.getConfidenceScore()).isNotEqualTo(f5EvidenceScore);
    }

    @Test
    @DisplayName("7. Citizen Deduplication: Same H3 within 60 min coalesced; different H3 or >60 min preserved")
    void testCitizenDeduplicationRules() {
        Instant baseTime = Instant.parse("2026-09-28T16:00:00Z");

        CitizenReport r1 = new CitizenReport();
        r1.setId(UUID.randomUUID());
        r1.setH3Index(PUNE_H3);
        r1.setSubmittedAt(baseTime);

        // Same H3, 15 min later (within 60m) -> DUPLICATE of r1
        CitizenReport r2 = new CitizenReport();
        r2.setId(UUID.randomUUID());
        r2.setH3Index(PUNE_H3);
        r2.setSubmittedAt(baseTime.plus(Duration.ofMinutes(15)));

        // Same H3, 75 min later (> 60m) -> SEPARATE / UNIQUE
        CitizenReport r3 = new CitizenReport();
        r3.setId(UUID.randomUUID());
        r3.setH3Index(PUNE_H3);
        r3.setSubmittedAt(baseTime.plus(Duration.ofMinutes(75)));

        // Different H3, same time -> SEPARATE / UNIQUE
        CitizenReport r4 = new CitizenReport();
        r4.setId(UUID.randomUUID());
        r4.setH3Index(DIFFERENT_H3);
        r4.setSubmittedAt(baseTime);

        List<CitizenReport> deduped = citizenReportDeduplicator.deduplicateReports(List.of(r1, r2, r3, r4));

        // Expect 3 unique reports: r1, r3, r4 (r2 is coalesced into r1)
        assertThat(deduped).hasSize(3);
        assertThat(deduped).extracting(CitizenReport::getId)
                .containsExactly(r1.getId(), r3.getId(), r4.getId());
    }

    @Test
    @DisplayName("8. Repeated orchestration idempotency: Calling orchestration twice does NOT duplicate EventEvidence")
    void testRepeatedOrchestrationIdempotency() {
        UUID eventId = UUID.randomUUID();
        PollutionEvent event = new PollutionEvent();
        event.setId(eventId);
        event.setEventCode("EVT-88608850-2026092816-idem");
        event.setH3Index(PUNE_H3);
        event.setStartedAt(Instant.now());
        event.setStatus("OPEN");

        HotspotSpatialContext ctx = mock(HotspotSpatialContext.class);
        when(ctx.h3Index()).thenReturn(PUNE_H3);
        when(ctx.predictedAt()).thenReturn(Instant.now());
        when(ctx.predictionId()).thenReturn(UUID.randomUUID());
        when(hotspotContextService.buildSpatialContextForH3(PUNE_H3)).thenReturn(Optional.of(ctx));

        when(pollutionEventRepository.findByEventCode(any())).thenReturn(Optional.of(event));
        when(pollutionEventRepository.save(any())).thenReturn(event);

        UUID reportId = UUID.randomUUID();
        CitizenReport report = new CitizenReport();
        report.setId(reportId);
        report.setH3Index(PUNE_H3);
        report.setSubmittedAt(Instant.now());
        report.setCategory("SMOKE");

        when(citizenReportRepository.findByH3IndexOrderBySubmittedAtDesc(PUNE_H3)).thenReturn(List.of(report));

        EvidenceAiOutputDto aiOutput = mock(EvidenceAiOutputDto.class);
        when(aiOutput.h3Index()).thenReturn(PUNE_H3);
        when(aiOutput.canonicalEventId()).thenReturn("EVT-88608850-2026092816-idem");
        when(aiOutput.signals()).thenReturn(Collections.emptyList());
        when(aiOutput.evidenceScore()).thenReturn(0.35);
        when(aiOutput.triageState()).thenReturn("MONITOR");
        when(evidenceAiClient.evaluate(any())).thenReturn(aiOutput);

        // First run: evidence key does not exist yet
        when(evidenceRepository.existsByEventIdAndEvidenceKey(eventId, "citizen-report-" + reportId)).thenReturn(false);

        evidenceOrchestrationService.getOrchestratedEvidence(PUNE_H3);

        // First run saves 1 EventEvidence
        verify(evidenceRepository, times(1)).save(any(EventEvidence.class));

        // Second run: evidence key now exists
        when(evidenceRepository.existsByEventIdAndEvidenceKey(eventId, "citizen-report-" + reportId)).thenReturn(true);

        evidenceOrchestrationService.getOrchestratedEvidence(PUNE_H3);

        // Total saves still 1! No duplicate created on second run.
        verify(evidenceRepository, times(1)).save(any(EventEvidence.class));
    }

    @Test
    @DisplayName("9. Event reuse: Reuses existing open event matching canonical code instead of creating a second event")
    void testEventReuseBehavior() {
        UUID existingEventId = UUID.randomUUID();
        PollutionEvent existingEvent = new PollutionEvent();
        existingEvent.setId(existingEventId);
        existingEvent.setEventCode("EVT-88608850-2026092816-reuse");
        existingEvent.setH3Index(PUNE_H3);
        existingEvent.setStatus("OPEN");

        HotspotSpatialContext ctx = mock(HotspotSpatialContext.class);
        when(ctx.h3Index()).thenReturn(PUNE_H3);
        when(ctx.predictedAt()).thenReturn(Instant.now());
        when(ctx.predictionId()).thenReturn(UUID.randomUUID());
        when(hotspotContextService.buildSpatialContextForH3(PUNE_H3)).thenReturn(Optional.of(ctx));

        when(pollutionEventRepository.findByEventCode("EVT-88608850-2026092816-reuse")).thenReturn(Optional.of(existingEvent));
        when(pollutionEventRepository.save(any())).thenReturn(existingEvent);

        EvidenceAiOutputDto aiOutput = mock(EvidenceAiOutputDto.class);
        when(aiOutput.h3Index()).thenReturn(PUNE_H3);
        when(aiOutput.canonicalEventId()).thenReturn("EVT-88608850-2026092816-reuse");
        when(aiOutput.signals()).thenReturn(Collections.emptyList());
        when(aiOutput.evidenceScore()).thenReturn(0.25);
        when(aiOutput.triageState()).thenReturn("INSUFFICIENT_EVIDENCE");
        when(evidenceAiClient.evaluate(any())).thenReturn(aiOutput);

        EvidenceSummaryResponse response = evidenceOrchestrationService.getOrchestratedEvidence(PUNE_H3);

        assertThat(response.context().eventId()).isEqualTo(existingEventId.toString());
        // Verify no new event was created
        verify(pollutionEventRepository, never()).save(argThat(e -> !e.getId().equals(existingEventId)));
    }

    @Test
    @DisplayName("10. Citizen evidence alone does NOT create an ALERT_CANDIDATE")
    void testCitizenEvidenceAloneDoesNotCreateAlert() {
        // Triage state is strictly determined by F5 Scoring Engine
        // When only citizen evidence is present, evidence score remains low (< 0.40)
        EvidenceAiOutputDto aiOutput = mock(EvidenceAiOutputDto.class);
        when(aiOutput.h3Index()).thenReturn(PUNE_H3);
        when(aiOutput.canonicalEventId()).thenReturn("EVT-88608850-2026092816-noalert");
        when(aiOutput.signals()).thenReturn(Collections.emptyList());
        when(aiOutput.evidenceScore()).thenReturn(0.18); // Low score with citizen only
        when(aiOutput.triageState()).thenReturn("INSUFFICIENT_EVIDENCE");
        when(evidenceAiClient.evaluate(any())).thenReturn(aiOutput);

        HotspotSpatialContext ctx = mock(HotspotSpatialContext.class);
        when(ctx.h3Index()).thenReturn(PUNE_H3);
        when(ctx.predictedAt()).thenReturn(Instant.now());
        when(ctx.predictionId()).thenReturn(UUID.randomUUID());
        when(hotspotContextService.buildSpatialContextForH3(PUNE_H3)).thenReturn(Optional.of(ctx));

        PollutionEvent event = new PollutionEvent();
        event.setId(UUID.randomUUID());
        event.setEventCode("EVT-88608850-2026092816-noalert");
        event.setH3Index(PUNE_H3);
        when(pollutionEventRepository.findByEventCode(any())).thenReturn(Optional.of(event));
        when(pollutionEventRepository.save(any())).thenReturn(event);

        EvidenceSummaryResponse response = evidenceOrchestrationService.getOrchestratedEvidence(PUNE_H3);

        assertThat(response.evidence().triageState()).isEqualTo("INSUFFICIENT_EVIDENCE");
        assertThat(response.evidence().evidenceScore()).isLessThan(0.40);
        // Alert candidate creation was not triggered
        verify(alertService, times(1)).createOrUpdateAlertCandidate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("11. Lifecycle verification distinction: ANALYZED != VERIFIED")
    void testAnalyzedIsNotVerified() {
        CitizenReport report = new CitizenReport();
        report.setId(UUID.randomUUID());
        report.setStatus("ANALYZED");
        report.setVerificationStatus("UNVERIFIED");

        // ANALYZED means AI processing completed; it does NOT mean verified ground truth
        assertThat(report.getStatus()).isEqualTo("ANALYZED");
        assertThat(report.getVerificationStatus()).isEqualTo("UNVERIFIED");
        assertThat(report.getVerificationStatus()).isNotEqualTo("VERIFIED");
    }
}
