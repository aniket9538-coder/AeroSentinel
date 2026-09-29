package com.aerosentinel.event;

import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.citizen.CitizenReport;
import com.aerosentinel.citizen.CitizenReportRepository;
import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.event.PollutionEventContextDto;
import com.aerosentinel.evidence.EventEvidence;
import com.aerosentinel.evidence.EvidenceRepository;
import com.aerosentinel.forecast.Forecast;
import com.aerosentinel.forecast.ForecastRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollutionEventUnitTest {

    @Mock
    private PollutionEventRepository eventRepository;

    @Mock
    private HotspotRepository hotspotRepository;

    @Mock
    private ForecastRepository forecastRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CitizenReportRepository citizenReportRepository;

    @Mock
    private GeminiAnalysisRepository geminiAnalysisRepository;

    @Mock
    private GridRepository gridRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private PollutionEventService eventService;

    private static final String TEST_H3 = "88608850e5fffff";
    private static final UUID TEST_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID TEST_GRID_ID = UUID.fromString("955f1f67-e5ff-48ef-87b6-133ff958b756");
    private static final UUID TEST_PRED_ID = UUID.fromString("a310c689-f340-49fc-8935-a037de8d7709");
    private static final UUID TEST_EVENT_ID = UUID.fromString("58ef2f64-4bc5-496f-9094-e5f61e44f8b0");
    private static final UUID TEST_REPORT_ID = UUID.fromString("6f3366cb-82cd-4a5f-b1fb-7eea6992a07d");

    private PollutionEvent testEvent;
    private HotspotPrediction testPrediction;
    private City testCity;
    private GridCell testGridCell;

    @BeforeEach
    void setUp() {
        testEvent = new PollutionEvent();
        testEvent.setId(TEST_EVENT_ID);
        testEvent.setEventCode("EVT-88608850-2026092816-f28bd5fe");
        testEvent.setH3Index(TEST_H3);
        testEvent.setGridCellId(TEST_GRID_ID);
        testEvent.setPredictionId(TEST_PRED_ID);
        testEvent.setSeverity("CRITICAL");
        testEvent.setStatus("OPEN");
        testEvent.setStartedAt(Instant.parse("2026-09-28T16:51:31Z"));
        testEvent.setCreatedAt(Instant.parse("2026-09-28T17:47:09Z"));

        testPrediction = new HotspotPrediction();
        testPrediction.setId(TEST_PRED_ID);
        testPrediction.setCityId(TEST_CITY_ID);
        testPrediction.setGridCellId(TEST_GRID_ID);
        testPrediction.setH3Index(TEST_H3);
        testPrediction.setRiskScore(0.7998);
        testPrediction.setRiskLevel("CRITICAL");
        testPrediction.setConfidence(0.86);
        testPrediction.setModelVersion("hotspot_classifier_v1");
        testPrediction.setPredictedAt(Instant.parse("2026-09-28T16:51:31Z"));

        testCity = new City();
        testCity.setId(TEST_CITY_ID);
        testCity.setName("Pune");
        testCity.setState("Maharashtra");
        testCity.setCountry("India");

        testGridCell = new GridCell();
        testGridCell.setId(TEST_GRID_ID);
        testGridCell.setCityId(TEST_CITY_ID);
        testGridCell.setH3Index(TEST_H3);
    }

    @Test
    @DisplayName("F7-P3-UNIT-01: Event context preserves F3 predictionId lineage exactly")
    void testEventPreservesF3PredictionLineage() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));
        when(gridRepository.findById(TEST_GRID_ID)).thenReturn(Optional.of(testGridCell));
        when(cityRepository.findById(TEST_CITY_ID)).thenReturn(Optional.of(testCity));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto);
        assertEquals(TEST_PRED_ID, dto.predictionId());
        assertNotNull(dto.prediction());
        assertEquals(TEST_PRED_ID, dto.prediction().predictionId());
    }

    @Test
    @DisplayName("F7-P3-UNIT-02: Event context preserves exact H3 spatial lineage")
    void testEventPreservesF3H3Lineage() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertEquals(TEST_H3, dto.h3Index());
        assertEquals(TEST_H3, dto.prediction().h3Index());
    }

    @Test
    @DisplayName("F7-P3-UNIT-03: Event context preserves authoritative F3 risk fields without recomputation")
    void testEventPreservesAuthoritativeRiskFields() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertEquals(0.7998, dto.prediction().riskScore());
        assertEquals("CRITICAL", dto.prediction().riskLevel());
        assertEquals(0.86, dto.prediction().confidence());
        assertEquals(0.20, dto.prediction().operationalThreshold());
        assertEquals("hotspot_classifier_v1", dto.prediction().modelVersion());
    }

    @Test
    @DisplayName("F7-P3-UNIT-04: Event context exposes F4 multi-horizon forecast when available")
    void testEventContextExposesF4ForecastWhenAvailable() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        Forecast fc1 = new Forecast(TEST_PRED_ID, TEST_CITY_ID, TEST_H3, TEST_GRID_ID, null,
                Instant.now(), Instant.now().plusSeconds(3600), 1, 70.62, 68.78, 72.48, 0.91, "forecast_regressors_v1", "ug/m3", "SUCCESS");
        Forecast fc3 = new Forecast(TEST_PRED_ID, TEST_CITY_ID, TEST_H3, TEST_GRID_ID, null,
                Instant.now(), Instant.now().plusSeconds(10800), 3, 70.55, 66.65, 73.60, 0.88, "forecast_regressors_v1", "ug/m3", "SUCCESS");
        Forecast fc6 = new Forecast(TEST_PRED_ID, TEST_CITY_ID, TEST_H3, TEST_GRID_ID, null,
                Instant.now(), Instant.now().plusSeconds(21600), 6, 60.91, 55.39, 66.33, 0.84, "forecast_regressors_v1", "ug/m3", "SUCCESS");

        when(forecastRepository.findByParentPredictionIdOrderByHorizonHoursAsc(TEST_PRED_ID))
                .thenReturn(List.of(fc1, fc3, fc6));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.forecast());
        assertTrue(dto.forecast().available());
        assertEquals("AVAILABLE", dto.forecast().status());
        assertEquals(3, dto.forecast().horizons().size());
        assertEquals(1, dto.forecast().horizons().get(0).horizonHours());
        assertEquals(70.62, dto.forecast().horizons().get(0).predictedPm25());
        assertEquals(3, dto.forecast().horizons().get(1).horizonHours());
        assertEquals(70.55, dto.forecast().horizons().get(1).predictedPm25());
        assertEquals(6, dto.forecast().horizons().get(2).horizonHours());
        assertEquals(60.91, dto.forecast().horizons().get(2).predictedPm25());
    }

    @Test
    @DisplayName("F7-P3-UNIT-05: Missing forecast handled honestly without inventing values")
    void testForecastUnavailableCaseDoesNotInventValues() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));
        when(forecastRepository.findByParentPredictionIdOrderByHorizonHoursAsc(TEST_PRED_ID))
                .thenReturn(Collections.emptyList());
        when(forecastRepository.findLatestByH3Index(TEST_H3)).thenReturn(Collections.emptyList());

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.forecast());
        assertFalse(dto.forecast().available());
        assertEquals("UNAVAILABLE", dto.forecast().status());
        assertTrue(dto.forecast().horizons().isEmpty());
    }

    @Test
    @DisplayName("F7-P3-UNIT-06: Event exposes F5 evidence state without recomputation")
    void testEventExposesF5EvidenceStateWithoutRecomputation() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setEventId(TEST_EVENT_ID);
        alert.setEvidenceScore(0.85);
        alert.setConsistency("high");
        alert.setTriageState("ALERT_CANDIDATE");
        when(alertRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(alert));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.evidence());
        assertEquals(0.85, dto.evidence().evidenceScore());
        assertEquals("high", dto.evidence().consistency());
        assertEquals("ALERT_CANDIDATE", dto.evidence().triageState());
    }

    @Test
    @DisplayName("F7-P3-UNIT-07: F5 evidenceScore remains strictly distinct from F3 riskScore")
    void testF5EvidenceScoreRemainsStrictlyDistinctFromF3RiskScore() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        // Pune runtime scenario: F3 riskScore = 0.7998, but F5 evidenceScore = 0.224 (INSUFFICIENT_EVIDENCE)
        EventEvidence ev1 = new EventEvidence();
        ev1.setId(UUID.randomUUID());
        ev1.setEventId(TEST_EVENT_ID);
        ev1.setDataSource("PUN-001");
        ev1.setSourceType("DIRECT_OBSERVATION");
        ev1.setRelevanceTier("PRIMARY");
        ev1.setConfidenceScore(0.9);
        ev1.setEvidenceValue("PM2.5: 78.0");
        when(evidenceRepository.findByEventIdOrderByCreatedAtAsc(TEST_EVENT_ID)).thenReturn(List.of(ev1));
        when(alertRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.empty());

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.prediction());
        assertNotNull(dto.evidence());
        assertEquals(0.7998, dto.prediction().riskScore());
        assertNotEquals(dto.prediction().riskScore(), dto.evidence().evidenceScore());
    }

    @Test
    @DisplayName("F7-P3-UNIT-08: Citizen EventEvidence remains strictly CITIZEN + AUXILIARY")
    void testCitizenEventEvidenceRemainsCitizenAndAuxiliary() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        EventEvidence citizenEv = new EventEvidence();
        citizenEv.setId(UUID.randomUUID());
        citizenEv.setEventId(TEST_EVENT_ID);
        citizenEv.setDataSource("CITIZEN");
        citizenEv.setSourceType("CITIZEN_OBSERVATION");
        citizenEv.setRelevanceTier("AUXILIARY");
        citizenEv.setSourceRef(TEST_REPORT_ID.toString());
        citizenEv.setEvidenceKey("citizen-report-" + TEST_REPORT_ID);
        citizenEv.setEvidenceValue("Industrial smokestack emissions observed near rail corridor.");
        citizenEv.setConfidenceScore(0.95);
        when(evidenceRepository.findByEventIdOrderByCreatedAtAsc(TEST_EVENT_ID)).thenReturn(List.of(citizenEv));

        CitizenReport report = new CitizenReport();
        report.setId(TEST_REPORT_ID);
        report.setCategory("INDUSTRIAL_EMISSION");
        report.setDescription("Industrial smokestack emissions observed near rail corridor.");
        report.setImageUrl("/api/v1/citizen/photos/photo-1.jpg");
        when(citizenReportRepository.findById(TEST_REPORT_ID)).thenReturn(Optional.of(report));

        GeminiAnalysis ga = new GeminiAnalysis();
        ga.setId(UUID.randomUUID());
        ga.setCitizenReportId(TEST_REPORT_ID);
        ga.setDetectedCategory("SMOKE_LIKE");
        ga.setConfidence(0.95);
        ga.setNarrativeSummary("Heavy industrial plume visible.");
        when(geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(TEST_REPORT_ID)).thenReturn(Optional.of(ga));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.citizenEvidence());
        assertEquals(1, dto.citizenEvidence().size());
        assertEquals("CITIZEN", dto.citizenEvidence().get(0).dataSource());
        assertEquals("AUXILIARY", dto.citizenEvidence().get(0).relevanceTier());
        assertEquals("SMOKE_LIKE", dto.citizenEvidence().get(0).visibleCondition());
        assertEquals(0.95, dto.citizenEvidence().get(0).visualConfidence());
        assertEquals("/api/v1/citizen/photos/photo-1.jpg", dto.citizenEvidence().get(0).photoUrl());
    }

    @Test
    @DisplayName("F7-P3-UNIT-09: Gemini visual confidence remains completely separate from F3/F5 metrics")
    void testGeminiVisualConfidenceRemainsSeparateFromF3AndF5Metrics() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        EventEvidence citizenEv = new EventEvidence();
        citizenEv.setId(UUID.randomUUID());
        citizenEv.setEventId(TEST_EVENT_ID);
        citizenEv.setDataSource("CITIZEN");
        citizenEv.setSourceType("CITIZEN_OBSERVATION");
        citizenEv.setRelevanceTier("AUXILIARY");
        citizenEv.setSourceRef(TEST_REPORT_ID.toString());
        citizenEv.setConfidenceScore(0.95);
        when(evidenceRepository.findByEventIdOrderByCreatedAtAsc(TEST_EVENT_ID)).thenReturn(List.of(citizenEv));

        CitizenReport report = new CitizenReport();
        report.setId(TEST_REPORT_ID);
        when(citizenReportRepository.findById(TEST_REPORT_ID)).thenReturn(Optional.of(report));

        GeminiAnalysis ga = new GeminiAnalysis();
        ga.setId(UUID.randomUUID());
        ga.setCitizenReportId(TEST_REPORT_ID);
        ga.setDetectedCategory("SMOKE_LIKE");
        ga.setConfidence(0.95);
        when(geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(TEST_REPORT_ID)).thenReturn(Optional.of(ga));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertEquals(0.7998, dto.prediction().riskScore());
        assertEquals(0.86, dto.prediction().confidence());
        assertEquals(0.95, dto.citizenEvidence().get(0).visualConfidence());
        // Invariant: Gemini confidence (0.95) does not corrupt F3 riskScore (0.7998)
        assertNotEquals(dto.citizenEvidence().get(0).visualConfidence(), dto.prediction().riskScore());
    }

    @Test
    @DisplayName("F7-P3-UNIT-10: Event can exist without Alert (Event != Alert)")
    void testEventCanExistWithoutAlert() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));
        when(alertRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.empty());

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto);
        assertEquals("OPEN", dto.status());
        assertEquals(TEST_EVENT_ID, dto.id());
        assertNotNull(dto.alert());
        assertFalse(dto.alert().alertExists());
        assertNull(dto.alert().alertId());
    }

    @Test
    @DisplayName("F7-P3-UNIT-11: Authoritative Alert candidate links seamlessly to Event context")
    void testAuthoritativeAlertLinksSeamlessly() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        UUID alertId = UUID.randomUUID();
        Alert alert = new Alert();
        alert.setId(alertId);
        alert.setEventId(TEST_EVENT_ID);
        alert.setStatus("OPEN");
        alert.setSeverity("CRITICAL");
        alert.setTriageState("ALERT_CANDIDATE");
        alert.setTitle("Critical Particulate Inversion");
        alert.setMessage("Exceeds emergency threshold");
        when(alertRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(alert));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.alert());
        assertTrue(dto.alert().alertExists());
        assertEquals(alertId, dto.alert().alertId());
        assertEquals("ALERT_CANDIDATE", dto.alert().triageState());
        assertEquals("Critical Particulate Inversion", dto.alert().title());
    }

    @Test
    @DisplayName("F7-P3-UNIT-12: Citizen photo path is safely transformed without internal filesystem leaks")
    void testCitizenPhotoPathSafelyTransformed() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        EventEvidence citizenEv = new EventEvidence();
        citizenEv.setId(UUID.randomUUID());
        citizenEv.setEventId(TEST_EVENT_ID);
        citizenEv.setDataSource("CITIZEN");
        citizenEv.setSourceType("CITIZEN_OBSERVATION");
        citizenEv.setRelevanceTier("AUXILIARY");
        citizenEv.setSourceRef(TEST_REPORT_ID.toString());
        when(evidenceRepository.findByEventIdOrderByCreatedAtAsc(TEST_EVENT_ID)).thenReturn(List.of(citizenEv));

        CitizenReport report = new CitizenReport();
        report.setId(TEST_REPORT_ID);
        // Stored with Windows/relative path
        report.setImageUrl("uploads\\citizen\\plume_sample.jpg");
        when(citizenReportRepository.findById(TEST_REPORT_ID)).thenReturn(Optional.of(report));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertEquals("/api/v1/citizen/photos/plume_sample.jpg", dto.citizenEvidence().get(0).photoUrl());
        assertFalse(dto.citizenEvidence().get(0).photoUrl().contains("\\"));
        assertFalse(dto.citizenEvidence().get(0).photoUrl().contains("uploads"));
    }

    @Test
    @DisplayName("F7-P4-UNIT-13: No citizen evidence returns an empty list without fake observations")
    void testNoCitizenEvidenceReturnsEmptyList() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));
        when(evidenceRepository.findByEventIdOrderByCreatedAtAsc(TEST_EVENT_ID)).thenReturn(Collections.emptyList());

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.citizenEvidence());
        assertTrue(dto.citizenEvidence().isEmpty());
    }

    @Test
    @DisplayName("F7-P4-UNIT-14: Invalid or non-existent event ID returns Optional.empty (404)")
    void testInvalidEventIdReturnsEmpty() {
        UUID unknownId = UUID.randomUUID();
        when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

        Optional<PollutionEventContextDto> res = eventService.getEventContextById(unknownId);

        assertTrue(res.isEmpty());
    }

    @Test
    @DisplayName("F7-P4-UNIT-15: Event context returns exact backend H3 index for spatial identity")
    void testEventContextH3ReturnedCorrectly() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto);
        assertEquals(TEST_H3, dto.h3Index());
    }

    @Test
    @DisplayName("F7-P4-UNIT-16: Alert context H3 matches parent event H3 cell exactly")
    void testAlertContextH3ReturnedCorrectly() {
        when(hotspotRepository.findById(TEST_PRED_ID)).thenReturn(Optional.of(testPrediction));

        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setEventId(TEST_EVENT_ID);
        alert.setStatus("OPEN");
        alert.setSeverity("HIGH");
        when(alertRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(alert));

        PollutionEventContextDto dto = eventService.toContextDto(testEvent);

        assertNotNull(dto.alert());
        assertTrue(dto.alert().alertExists());
        assertEquals(TEST_H3, dto.h3Index());
    }
}
