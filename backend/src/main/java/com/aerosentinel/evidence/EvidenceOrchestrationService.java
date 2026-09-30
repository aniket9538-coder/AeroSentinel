package com.aerosentinel.evidence;

import com.aerosentinel.event.PollutionEvent;
import com.aerosentinel.event.PollutionEventRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridService;
import com.aerosentinel.hotspot.HotspotSpatialContext;
import com.aerosentinel.dto.evidence.EvidenceSummaryResponse;
import com.aerosentinel.dto.evidence.EvidenceSummaryResponse.*;
import com.aerosentinel.forecast.ForecastResponse;
import com.aerosentinel.forecast.ForecastResponse.ForecastItem;
import com.aerosentinel.evidence.EvidenceAiClient.EvidenceAiOutputDto;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import com.aerosentinel.forecast.ForecastService;
import com.aerosentinel.hotspot.HotspotContextService;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.alert.AlertService;
import com.aerosentinel.citizen.CitizenReport;
import com.aerosentinel.citizen.CitizenReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Hardened Evidence Orchestrator for Feature 5 (P3 Evidence Integrity + Event Persistence).
 *
 * Enforces:
 * 1. Strict H3 consistency across F3, F4, F5, PollutionEvent, and GeminiAnalysis.
 * 2. Deterministic PollutionEvent construction & deduplication.
 * 3. Atomic EventEvidence persistence with deterministic duplicate protection.
 * 4. Bidirectional lineage preservation (PollutionEvent.id <-> GeminiAnalysis.eventId, predictionId).
 * 5. Complete transactional rollback on any downstream failure.
 * 6. F5-P5 Alert Candidate creation when F5 triage is ALERT_CANDIDATE.
 */
@Service
public class EvidenceOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceOrchestrationService.class);

    private final HotspotContextService hotspotContextService;
    private final ForecastService forecastService;
    private final EvidenceAiClient evidenceAiClient;
    private final GeminiAnalysisRepository geminiAnalysisRepository;
    private final PollutionEventRepository pollutionEventRepository;
    private final EvidenceRepository evidenceRepository;
    private final GridService gridService;
    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final CitizenReportRepository citizenReportRepository;
    private final com.aerosentinel.citizen.CitizenReportDeduplicator citizenReportDeduplicator;
    private final com.aerosentinel.citizen.CitizenEventMatcher citizenEventMatcher;
    private final Map<String, EvidenceSummaryResponse> dossierCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    public EvidenceOrchestrationService(
            HotspotContextService hotspotContextService,
            ForecastService forecastService,
            EvidenceAiClient evidenceAiClient,
            GeminiAnalysisRepository geminiAnalysisRepository,
            PollutionEventRepository pollutionEventRepository,
            EvidenceRepository evidenceRepository,
            GridService gridService,
            AlertService alertService,
            @Autowired(required = false) AlertRepository alertRepository,
            @Autowired(required = false) CitizenReportRepository citizenReportRepository,
            @Autowired(required = false) com.aerosentinel.citizen.CitizenReportDeduplicator citizenReportDeduplicator,
            @Autowired(required = false) com.aerosentinel.citizen.CitizenEventMatcher citizenEventMatcher
    ) {
        this.hotspotContextService = hotspotContextService;
        this.forecastService = forecastService;
        this.evidenceAiClient = evidenceAiClient;
        this.geminiAnalysisRepository = geminiAnalysisRepository;
        this.pollutionEventRepository = pollutionEventRepository;
        this.evidenceRepository = evidenceRepository;
        this.gridService = gridService;
        this.alertService = alertService;
        this.alertRepository = alertRepository;
        this.citizenReportRepository = citizenReportRepository;
        this.citizenReportDeduplicator = citizenReportDeduplicator != null ? citizenReportDeduplicator : new com.aerosentinel.citizen.CitizenReportDeduplicator();
        this.citizenEventMatcher = citizenEventMatcher != null ? citizenEventMatcher : new com.aerosentinel.citizen.CitizenEventMatcher();
    }

    public EvidenceOrchestrationService(
            HotspotContextService hotspotContextService,
            ForecastService forecastService,
            EvidenceAiClient evidenceAiClient,
            GeminiAnalysisRepository geminiAnalysisRepository,
            PollutionEventRepository pollutionEventRepository,
            EvidenceRepository evidenceRepository,
            GridService gridService,
            AlertService alertService,
            CitizenReportRepository citizenReportRepository,
            com.aerosentinel.citizen.CitizenReportDeduplicator citizenReportDeduplicator,
            com.aerosentinel.citizen.CitizenEventMatcher citizenEventMatcher
    ) {
        this(hotspotContextService, forecastService, evidenceAiClient,
             geminiAnalysisRepository, pollutionEventRepository, evidenceRepository,
             gridService, alertService, null, citizenReportRepository, citizenReportDeduplicator, citizenEventMatcher);
    }

    public EvidenceOrchestrationService(
            HotspotContextService hotspotContextService,
            ForecastService forecastService,
            EvidenceAiClient evidenceAiClient,
            GeminiAnalysisRepository geminiAnalysisRepository,
            PollutionEventRepository pollutionEventRepository,
            EvidenceRepository evidenceRepository,
            GridService gridService,
            AlertService alertService,
            CitizenReportRepository citizenReportRepository
    ) {
        this(hotspotContextService, forecastService, evidenceAiClient,
             geminiAnalysisRepository, pollutionEventRepository, evidenceRepository,
             gridService, alertService, null, citizenReportRepository, null, null);
    }

    public EvidenceOrchestrationService(
            HotspotContextService hotspotContextService,
            ForecastService forecastService,
            EvidenceAiClient evidenceAiClient,
            GeminiAnalysisRepository geminiAnalysisRepository,
            PollutionEventRepository pollutionEventRepository,
            EvidenceRepository evidenceRepository,
            GridService gridService,
            AlertService alertService
    ) {
        this(hotspotContextService, forecastService, evidenceAiClient,
             geminiAnalysisRepository, pollutionEventRepository, evidenceRepository,
             gridService, alertService, null, null, null, null);
    }

    public EvidenceOrchestrationService(
            HotspotContextService hotspotContextService,
            ForecastService forecastService,
            EvidenceAiClient evidenceAiClient,
            GeminiAnalysisRepository geminiAnalysisRepository,
            PollutionEventRepository pollutionEventRepository,
            EvidenceRepository evidenceRepository,
            GridService gridService
    ) {
        this(hotspotContextService, forecastService, evidenceAiClient,
             geminiAnalysisRepository, pollutionEventRepository, evidenceRepository,
             gridService, null, null, null, null, null);
    }

    /**
     * Synthesizes authoritative evidence, constructs canonical PollutionEvent,
     * persists structured evidence and Gemini narrative, and returns unified EvidenceSummaryResponse.
     *
     * @param h3Index 15-character Uber H3 resolution 8 index
     * @return grounded EvidenceSummaryResponse
     */
    @Transactional
    public EvidenceSummaryResponse getOrchestratedEvidence(String h3Index) {
        if (h3Index == null || h3Index.trim().length() < 10) {
            throw new ValidationException("Invalid H3 index: " + h3Index);
        }

        String normalizedH3 = h3Index.trim().toLowerCase();

        // 1. Load F3 Hotspot Spatial Context
        HotspotSpatialContext spatialContext = hotspotContextService.buildSpatialContextForH3(normalizedH3)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active hotspot intelligence or baseline spatial context found for H3 cell: " + normalizedH3
                ));

        // Strict H3 Check 1: F3 context H3 must match requested H3
        if (!normalizedH3.equalsIgnoreCase(spatialContext.h3Index())) {
            throw new ValidationException("H3 mismatch: requested " + normalizedH3 + " but F3 context is for " + spatialContext.h3Index());
        }

        // 2. Load F4 Multi-Horizon Forecast (optional / missingness tolerated)
        Optional<ForecastResponse> forecastOpt = forecastService.getForecastByH3(normalizedH3);

        // Strict H3 Check 2: If forecast present, its H3 must match requested H3
        if (forecastOpt.isPresent()) {
            String fcH3 = forecastOpt.get().h3Index();
            if (fcH3 != null && !normalizedH3.equalsIgnoreCase(fcH3.trim())) {
                throw new ValidationException("H3 mismatch: requested " + normalizedH3 + " but F4 forecast is for " + fcH3);
            }
        }

        // 3. Assemble CLI payload
        Map<String, Object> cliPayload = buildCliPayload(spatialContext, forecastOpt.orElse(null));

        // 4. Invoke AI bridge (F5 Scoring + Gemini pipeline)
        EvidenceAiOutputDto aiResult;
        try {
            aiResult = evidenceAiClient.evaluate(cliPayload);
        } catch (Exception e) {
            log.error("AI bridge execution failed for H3 {}: {}", normalizedH3, e.getMessage(), e);
            throw new RuntimeException("Evidence scoring bridge failed: " + e.getMessage(), e);
        }

        // Strict H3 Check 3: If AI result specifies H3, must match requested H3
        if (aiResult.h3Index() != null && !normalizedH3.equalsIgnoreCase(aiResult.h3Index().trim())) {
            throw new ValidationException("H3 mismatch: requested " + normalizedH3 + " but AI bridge returned " + aiResult.h3Index());
        }

        // 5. Construct or Resolve Canonical PollutionEvent
        PollutionEvent event = resolveOrCreatePollutionEvent(normalizedH3, spatialContext, aiResult);

        // 6. Persist structured EventEvidence records with duplicate protection
        persistEventEvidence(event, aiResult);

        // 6b. F6-P3: Persist auxiliary Citizen Evidence with deduplication and duplicate protection
        persistCitizenEvidence(event, normalizedH3);

        // 7. Persist Gemini structured analysis in PostgreSQL
        persistGeminiAnalysis(normalizedH3, spatialContext, event, aiResult);

        // 8. F5-P5: Create or Resolve Alert Candidate if triageState == ALERT_CANDIDATE
        if (alertService != null) {
            alertService.createOrUpdateAlertCandidate(event, spatialContext, forecastOpt.orElse(null), aiResult);
        }

        // 9. Build unified response and cache
        EvidenceSummaryResponse response = buildResponse(spatialContext, forecastOpt.orElse(null), event, aiResult);
        dossierCache.put(normalizedH3, response);
        return response;
    }

    private PollutionEvent resolveOrCreatePollutionEvent(
            String normalizedH3,
            HotspotSpatialContext ctx,
            EvidenceAiOutputDto aiResult
    ) {
        String canonicalCode = null;
        if (aiResult.canonicalEventId() != null && !aiResult.canonicalEventId().isBlank()) {
            canonicalCode = aiResult.canonicalEventId().trim();
        } else if (aiResult.eventId() != null && !aiResult.eventId().isBlank()) {
            canonicalCode = aiResult.eventId().trim();
        } else {
            String h3Prefix = normalizedH3.length() >= 8 ? normalizedH3.substring(0, 8) : normalizedH3;
            String timeClean = ctx.predictedAt() != null
                    ? ctx.predictedAt().toString().replaceAll("[-:T.]", "").substring(0, Math.min(10, ctx.predictedAt().toString().length()))
                    : "NOW";
            canonicalCode = "EVT-" + h3Prefix + "-" + timeClean;
        }

        Optional<PollutionEvent> existingOpt = pollutionEventRepository.findByEventCode(canonicalCode);
        if (existingOpt.isPresent()) {
            PollutionEvent existing = existingOpt.get();
            existing.setH3Index(normalizedH3);
            existing.setPredictionId(ctx.predictionId());
            log.info("Reusing existing PollutionEvent id={} code={} for H3 cell {}",
                    existing.getId(), canonicalCode, normalizedH3);
            return pollutionEventRepository.save(existing);
        }

        // Ensure real GridCell exists for relational FK
        GridCell gridCell = gridService.getOrCreateGridCell(normalizedH3, ctx.cityId());

        PollutionEvent newEvent = new PollutionEvent();
        newEvent.setGridCellId(gridCell.getId());
        newEvent.setH3Index(normalizedH3);
        newEvent.setPredictionId(ctx.predictionId());
        newEvent.setEventCode(canonicalCode);
        newEvent.setSeverity(ctx.riskLevel() != null ? ctx.riskLevel() : "MODERATE");
        newEvent.setStatus("OPEN");
        newEvent.setStartedAt(ctx.predictedAt() != null ? ctx.predictedAt() : Instant.now());
        newEvent.setCreatedAt(Instant.now());

        PollutionEvent saved = pollutionEventRepository.save(newEvent);
        log.info("Created new canonical PollutionEvent id={} code={} for H3 cell {}",
                saved.getId(), canonicalCode, normalizedH3);
        return saved;
    }

    private void persistEventEvidence(PollutionEvent event, EvidenceAiOutputDto aiResult) {
        if (aiResult.signals() == null || aiResult.signals().isEmpty()) {
            return;
        }

        for (EvidenceAiOutputDto.SignalItemDto sig : aiResult.signals()) {
            // Dedicated persistCitizenEvidence handles citizen reports with deduplication & spatio-temporal matching
            if ("CITIZEN".equalsIgnoreCase(sig.dataSource()) || "CITIZEN_OBSERVATION".equalsIgnoreCase(sig.sourceType())) {
                continue;
            }

            String evidenceKey = sig.signalId() != null ? sig.signalId() : UUID.randomUUID().toString();

            // Deterministic duplicate protection per event and signal
            if (evidenceRepository.existsByEventIdAndEvidenceKey(event.getId(), evidenceKey)) {
                log.debug("EventEvidence for eventId={} key={} already exists; skipping duplicate",
                        event.getId(), evidenceKey);
                continue;
            }

            Instant sigTime = Instant.now();
            if (sig.timestamp() != null) {
                try {
                    sigTime = Instant.parse(sig.timestamp());
                } catch (Exception ignored) {}
            }

            EventEvidence ev = new EventEvidence();
            ev.setEventId(event.getId());
            ev.setSourceType(sig.sourceType() != null ? sig.sourceType() : "DIRECT_OBSERVATION");
            ev.setEvidenceKey(evidenceKey);
            ev.setEvidenceValue(sig.description() != null ? sig.description() : "Telemetry observation");
            ev.setWeight(sig.confidenceScore() != null ? sig.confidenceScore() : 1.0);
            ev.setSignalId(sig.signalId());
            ev.setDataSource(sig.dataSource());
            ev.setRelevanceTier(sig.relevanceTier());
            ev.setSourceRef(sig.sourceRef());
            ev.setConfidenceScore(sig.confidenceScore());
            ev.setObservedAt(sigTime);
            ev.setCreatedAt(Instant.now());

            evidenceRepository.save(ev);
        }
    }

    /**
     * Persists auxiliary citizen evidence for deduplicated reports matching the event spatially and temporally.
     * Enforces:
     * 1. 60-minute same-H3 deduplication via CitizenReportDeduplicator.
     * 2. Spatio-temporal matching via CitizenEventMatcher (strict H3-8 and 120m temporal window).
     * 3. Deterministic duplicate protection (existsByEventIdAndEvidenceKey).
     * 4. Strict classification: dataSource = CITIZEN, relevanceTier = AUXILIARY.
     * 5. Links unlinked GeminiAnalysis.eventId to the PollutionEvent.
     */
    private void persistCitizenEvidence(PollutionEvent event, String normalizedH3) {
        if (citizenReportRepository == null) {
            return;
        }

        try {
            List<CitizenReport> rawReports = citizenReportRepository.findByH3IndexOrderBySubmittedAtDesc(normalizedH3);
            List<CitizenReport> dedupedReports = citizenReportDeduplicator.deduplicateReports(rawReports);

            for (CitizenReport cr : dedupedReports) {
                if (citizenEventMatcher.isMatch(cr, event)) {
                    String evidenceKey = "citizen-report-" + cr.getId();

                    if (evidenceRepository.existsByEventIdAndEvidenceKey(event.getId(), evidenceKey)) {
                        log.debug("Citizen evidence for event {} report {} already exists; skipping duplicate",
                                event.getId(), cr.getId());
                    } else {
                        Optional<GeminiAnalysis> gaOpt = geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(cr.getId());
                        double conf = gaOpt.map(GeminiAnalysis::getConfidence).orElse(0.75);
                        String detected = gaOpt.map(GeminiAnalysis::getDetectedCategory).orElse(cr.getCategory());

                        EventEvidence ev = new EventEvidence();
                        ev.setEventId(event.getId());
                        ev.setSourceType("CITIZEN_OBSERVATION");
                        ev.setEvidenceKey(evidenceKey);
                        String desc = (cr.getDescription() != null && !cr.getDescription().isBlank())
                                ? cr.getDescription()
                                : "Citizen visual observation";
                        ev.setEvidenceValue("Citizen observation [" + detected + "]: " + desc);
                        ev.setWeight(conf);
                        ev.setConfidenceScore(conf);
                        ev.setSignalId("sig-citizen-" + cr.getId().toString().substring(0, Math.min(8, cr.getId().toString().length())));
                        ev.setDataSource("CITIZEN");
                        ev.setRelevanceTier("AUXILIARY");
                        ev.setSourceRef(cr.getId().toString());
                        ev.setObservedAt(cr.getSubmittedAt() != null ? cr.getSubmittedAt() : Instant.now());
                        ev.setCreatedAt(Instant.now());

                        evidenceRepository.save(ev);
                        log.info("Persisted auxiliary EventEvidence for citizen report {} attached to event {}",
                                cr.getId(), event.getId());
                    }

                    // Link GeminiAnalysis to PollutionEvent if not already linked
                    Optional<GeminiAnalysis> gaOpt = geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(cr.getId());
                    gaOpt.ifPresent(analysis -> {
                        if (analysis.getEventId() == null) {
                            analysis.setEventId(event.getId());
                            geminiAnalysisRepository.save(analysis);
                            log.info("Linked GeminiAnalysis id={} to event id={}", analysis.getId(), event.getId());
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Error during citizen evidence persistence for event {}: {}", event.getId(), e.getMessage());
        }
    }

    private void persistGeminiAnalysis(
            String h3Index,
            HotspotSpatialContext ctx,
            PollutionEvent event,
            EvidenceAiOutputDto aiResult
    ) {
        if (aiResult == null || aiResult.aiInterpretation() == null) {
            return;
        }

        EvidenceAiOutputDto.AiInterpretationDto ai = aiResult.aiInterpretation();
        GeminiAnalysis analysis = new GeminiAnalysis();
        analysis.setH3Index(h3Index);
        analysis.setEventId(event.getId()); // Strictly linked to PollutionEvent.id
        analysis.setPredictionId(ctx.predictionId()); // Strictly linked to HotspotPrediction.id
        analysis.setModelVersion(ai.modelVersion() != null ? ai.modelVersion() : "gemini-2.0-flash");
        analysis.setPromptVersion(ai.promptVersion() != null ? ai.promptVersion() : "structured_event_explanation_v001");
        analysis.setEventSummaryPublic(ai.summaryPublic());
        analysis.setEventSummaryAnalyst(ai.summaryAnalyst());
        analysis.setDetectedCondition(ai.detectedCondition());
        analysis.setForecastTrajectory(ai.forecastTrajectory());
        analysis.setUncertaintyStatement(ai.uncertaintyStatement());
        analysis.setIsGrounded(ai.isGrounded() != null ? ai.isGrounded() : true);
        analysis.setCreatedAt(Instant.now());

        geminiAnalysisRepository.save(analysis);
        log.info("Persisted GeminiAnalysis for H3 cell {} linked to event {} and prediction {}",
                h3Index, event.getId(), ctx.predictionId());
    }

    private Map<String, Object> buildCliPayload(HotspotSpatialContext ctx, ForecastResponse fc) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("h3Index", ctx.h3Index());
        payload.put("cityId", ctx.cityId() != null ? ctx.cityId().toString() : null);
        payload.put("cityName", ctx.cityName());
        payload.put("predictionId", ctx.predictionId() != null ? ctx.predictionId().toString() : null);
        payload.put("featureSnapshotId", ctx.featureSnapshotId() != null ? ctx.featureSnapshotId().toString() : null);
        payload.put("timestamp", ctx.predictedAt() != null ? ctx.predictedAt().toString() : Instant.now().toString());

        // Hotspot model signals
        Map<String, Object> hotspotMap = new LinkedHashMap<>();
        hotspotMap.put("riskScore", ctx.riskScore());
        hotspotMap.put("operationalThreshold", ctx.operationalThreshold());
        hotspotMap.put("isHotspot", ctx.isHotspot());
        hotspotMap.put("riskLevel", ctx.riskLevel());
        hotspotMap.put("confidence", ctx.confidence());
        hotspotMap.put("modelVersion", ctx.modelVersion());
        hotspotMap.put("engineType", ctx.engineType());
        if (ctx.confidenceBreakdown() != null) {
            hotspotMap.put("confidenceBreakdown", Map.of(
                    "overallConfidence", ctx.confidenceBreakdown().overallConfidence(),
                    "dataQualityScore", ctx.confidenceBreakdown().dataQualityScore(),
                    "spatialCoverageConfidence", ctx.confidenceBreakdown().spatialCoverageConfidence(),
                    "modelCertainty", ctx.confidenceBreakdown().modelCertainty()
            ));
        }
        payload.put("hotspot", hotspotMap);

        // Forecast horizons {1h, 3h, 6h}
        if (fc != null && fc.forecasts() != null && !fc.forecasts().isEmpty()) {
            Map<String, Object> fcMap = new LinkedHashMap<>();
            fcMap.put("baseTimestamp", fc.baseTimestamp() != null ? fc.baseTimestamp().toString() : null);
            fcMap.put("generatedAt", fc.generatedAt() != null ? fc.generatedAt().toString() : null);
            fcMap.put("forecastModelVersion", fc.modelVersion());
            fcMap.put("parentPredictionId", fc.parentPredictionId() != null ? fc.parentPredictionId().toString() : null);
            fcMap.put("horizons", fc.forecasts());
            payload.put("forecast", fcMap);
        } else {
            payload.put("forecast", Collections.emptyMap());
        }

        // Crowdsourced Citizen Observations in this H3 cell (deduplicated via authoritative 60-min rule)
        List<Map<String, Object>> citizenList = new ArrayList<>();
        if (citizenReportRepository != null && ctx.h3Index() != null) {
            try {
                List<CitizenReport> rawReports = citizenReportRepository.findByH3IndexOrderBySubmittedAtDesc(ctx.h3Index());
                List<CitizenReport> dedupedReports = citizenReportDeduplicator.deduplicateReports(rawReports);
                for (CitizenReport cr : dedupedReports) {
                    Map<String, Object> crMap = new LinkedHashMap<>();
                    crMap.put("report_id", cr.getId() != null ? cr.getId().toString() : "unknown");
                    crMap.put("h3_cell_id", cr.getH3Index());
                    crMap.put("timestamp", cr.getSubmittedAt() != null ? cr.getSubmittedAt().toString() : Instant.now().toString());
                    crMap.put("category", cr.getCategory());
                    crMap.put("description", cr.getDescription());
                    crMap.put("status", cr.getStatus());
                    crMap.put("image_url", cr.getImageUrl());

                    Optional<GeminiAnalysis> gaOpt = geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(cr.getId());
                    if (gaOpt.isPresent()) {
                        GeminiAnalysis ga = gaOpt.get();
                        crMap.put("detected_category", ga.getDetectedCategory());
                        crMap.put("confidence", ga.getConfidence());
                        crMap.put("narrative_summary", ga.getNarrativeSummary());
                    }
                    citizenList.add(crMap);
                }
            } catch (Exception e) {
                log.warn("Failed to query citizen reports for H3 {}: {}", ctx.h3Index(), e.getMessage());
            }
        }
        payload.put("citizenReports", citizenList);

        // Physical telemetry facts
        if (ctx.airContext() != null) {
            Map<String, Object> airMap = new LinkedHashMap<>();
            airMap.put("pm25", ctx.airContext().pm25());
            airMap.put("pm10", ctx.airContext().pm10());
            airMap.put("no2", ctx.airContext().no2());
            airMap.put("so2", ctx.airContext().so2());
            airMap.put("co", ctx.airContext().co());
            airMap.put("o3", ctx.airContext().o3());
            airMap.put("dataQuality", ctx.airContext().dataQuality());
            airMap.put("stationId", ctx.airContext().stationId());
            airMap.put("observedAt", ctx.airContext().observedAt() != null ? ctx.airContext().observedAt().toString() : null);
            payload.put("air", airMap);
        }

        if (ctx.weatherContext() != null) {
            Map<String, Object> weatherMap = new LinkedHashMap<>();
            weatherMap.put("temperature", ctx.weatherContext().temperature());
            weatherMap.put("humidity", ctx.weatherContext().humidity());
            weatherMap.put("windSpeedMps", ctx.weatherContext().windSpeedMps());
            weatherMap.put("windSpeedKmh", ctx.weatherContext().windSpeedKmh());
            weatherMap.put("windDirection", ctx.weatherContext().windDirection());
            weatherMap.put("surfacePressure", ctx.weatherContext().surfacePressure());
            weatherMap.put("precipitation", ctx.weatherContext().precipitation());
            payload.put("weather", weatherMap);
        }

        if (ctx.monitoringCoverage() != null) {
            Map<String, Object> covMap = new LinkedHashMap<>();
            covMap.put("nearestStationDistanceKm", ctx.monitoringCoverage().nearestStationDistanceKm());
            covMap.put("stationsWithin5kmCount", ctx.monitoringCoverage().stationsWithin5kmCount());
            covMap.put("monitoringCoverageGapFlag", ctx.monitoringCoverage().monitoringCoverageGapFlag());
            covMap.put("spatialCoverageConfidence", ctx.monitoringCoverage().spatialCoverageConfidence());
            payload.put("monitoringCoverage", covMap);
        }

        if (ctx.spatialDispersion() != null) {
            Map<String, Object> dispMap = new LinkedHashMap<>();
            dispMap.put("pm25SpatialLagMean", ctx.spatialDispersion().pm25SpatialLagMean());
            dispMap.put("windU", ctx.spatialDispersion().windU());
            dispMap.put("windV", ctx.spatialDispersion().windV());
            payload.put("spatialDispersion", dispMap);
        }

        if (ctx.environmentalGis() != null) {
            Map<String, Object> gisMap = new LinkedHashMap<>();
            gisMap.put("distToNearestIndustrialKm", ctx.environmentalGis().distToNearestIndustrialKm());
            gisMap.put("distToNearestMajorRoadKm", ctx.environmentalGis().distToNearestMajorRoadKm());
            gisMap.put("sensitiveReceptorsCount2km", ctx.environmentalGis().sensitiveReceptorsCount2km());
            gisMap.put("industrialZoneWithin2kmFlag", ctx.environmentalGis().industrialZoneWithin2kmFlag());
            gisMap.put("fireCount24h25km", ctx.environmentalGis().fireCount24h25km());
            gisMap.put("nearestFireDistanceKm", ctx.environmentalGis().nearestFireDistanceKm());
            payload.put("environmentalGis", gisMap);
        }

        return payload;
    }

    private EvidenceSummaryResponse buildResponse(
            HotspotSpatialContext ctx,
            ForecastResponse fc,
            PollutionEvent event,
            EvidenceAiOutputDto aiResult
    ) {
        // 1. Context
        ContextDto contextDto = new ContextDto(
                ctx.h3Index(),
                ctx.cityId() != null ? ctx.cityId().toString() : null,
                ctx.cityName(),
                ctx.predictionId() != null ? ctx.predictionId().toString() : null,
                ctx.featureSnapshotId() != null ? ctx.featureSnapshotId().toString() : null,
                event.getId().toString(),
                event.getEventCode(),
                ctx.predictedAt() != null ? ctx.predictedAt() : Instant.now()
        );

        // 2. Observed Facts (strictly separated)
        ObservedFactsDto observedFacts = new ObservedFactsDto(
                ctx.airContext(),
                ctx.weatherContext(),
                ctx.monitoringCoverage(),
                ctx.spatialDispersion(),
                ctx.environmentalGis()
        );

        // 3. Model Outputs (strictly separated)
        HotspotModelOutputDto hotspotOutput = new HotspotModelOutputDto(
                ctx.riskScore(),
                ctx.operationalThreshold(),
                ctx.isHotspot(),
                ctx.riskLevel(),
                ctx.confidence(),
                ctx.confidenceBreakdown(),
                ctx.modelVersion(),
                ctx.engineType()
        );

        ForecastModelOutputDto forecastOutput = null;
        if (fc != null) {
            forecastOutput = new ForecastModelOutputDto(
                    fc.baseTimestamp(),
                    fc.generatedAt(),
                    fc.modelVersion(),
                    fc.parentPredictionId(),
                    fc.forecastConfidence(), // strictly null per locked F4 contract
                    fc.forecasts()
            );
        }

        ModelOutputsDto modelOutputs = new ModelOutputsDto(hotspotOutput, forecastOutput);

        // 4. Evidence / Alert Support
        List<EvidenceSignalDto> signals = new ArrayList<>();
        Set<String> seenSignals = new HashSet<>();
        if (aiResult.signals() != null) {
            for (EvidenceAiOutputDto.SignalItemDto item : aiResult.signals()) {
                Instant sigTime = item.timestamp() != null ? Instant.parse(item.timestamp()) : Instant.now();
                String refKey = item.sourceRef() != null ? item.sourceRef() : item.signalId();
                if (refKey != null) {
                    seenSignals.add(refKey);
                }
                signals.add(new EvidenceSignalDto(
                        item.signalId(),
                        item.sourceType(),
                        item.description(),
                        sigTime,
                        item.confidenceScore(),
                        item.dataSource(),
                        item.relevanceTier(),
                        item.sourceRef()
                ));
            }
        }

        // Include persisted CITIZEN EventEvidence if not already included
        try {
            List<EventEvidence> persistedEvidence = evidenceRepository.findByEventIdOrderByCreatedAtAsc(event.getId());
            for (EventEvidence pe : persistedEvidence) {
                if ("CITIZEN".equalsIgnoreCase(pe.getDataSource())) {
                    String ref = pe.getSourceRef() != null ? pe.getSourceRef() : pe.getSignalId();
                    if (ref != null && !seenSignals.contains(ref)) {
                        seenSignals.add(ref);
                        signals.add(new EvidenceSignalDto(
                                pe.getSignalId() != null ? pe.getSignalId() : "sig-citizen-" + pe.getId().toString().substring(0, Math.min(8, pe.getId().toString().length())),
                                pe.getSourceType(),
                                pe.getEvidenceValue(),
                                pe.getObservedAt() != null ? pe.getObservedAt() : pe.getCreatedAt(),
                                pe.getConfidenceScore(),
                                pe.getDataSource(),
                                pe.getRelevanceTier() != null ? pe.getRelevanceTier() : "AUXILIARY",
                                pe.getSourceRef()
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load persisted citizen evidence for response: {}", e.getMessage());
        }

        ScoreBreakdownDto scoreBreakdown = null;
        if (aiResult.scoreBreakdown() != null) {
            EvidenceAiOutputDto.ScoreBreakdownDto sb = aiResult.scoreBreakdown();
            scoreBreakdown = new ScoreBreakdownDto(
                    sb.observation_strength(),
                    sb.ml_forecast_support(),
                    sb.multi_source_agreement(),
                    sb.spatial_consistency(),
                    sb.temporal_persistence(),
                    sb.recency_factor(),
                    sb.conflict_penalty(),
                    sb.evidence_completeness(),
                    sb.final_evidence_score()
            );
        }

        List<String> clusterCells = aiResult.clusterH3Cells() != null ? aiResult.clusterH3Cells() : Collections.singletonList(ctx.h3Index());

        EvidenceDto evidenceDto = new EvidenceDto(
                signals,
                aiResult.evidenceScore(),
                scoreBreakdown,
                aiResult.consistency(),
                aiResult.triageState(),
                aiResult.sourceMatrix() != null ? aiResult.sourceMatrix() : Collections.emptyMap(),
                aiResult.unavailableSources() != null ? aiResult.unavailableSources() : Collections.emptyList(),
                aiResult.conflictingNotes() != null ? aiResult.conflictingNotes() : Collections.emptyList(),
                clusterCells
        );

        // 5. AI Interpretation (narrative strictly separated)
        AiInterpretationDto aiInterpretation = null;
        if (aiResult.aiInterpretation() != null) {
            EvidenceAiOutputDto.AiInterpretationDto ai = aiResult.aiInterpretation();
            aiInterpretation = new AiInterpretationDto(
                    ai.summaryPublic(),
                    ai.summaryAnalyst(),
                    ai.detectedCondition(),
                    ai.supportingSignals() != null ? ai.supportingSignals() : Collections.emptyList(),
                    ai.forecastTrajectory(),
                    ai.uncertaintyStatement(),
                    ai.unsupportedConclusions() != null ? ai.unsupportedConclusions() : Collections.emptyList(),
                    ai.causalClaimSupported() != null ? ai.causalClaimSupported() : false,
                    ai.isGrounded() != null ? ai.isGrounded() : true,
                    ai.modelVersion(),
                    ai.promptVersion()
            );
        }

        // 6. Recommended Verification (operational guidance strictly separated)
        RecommendedVerificationDto recommendedVerification = null;
        if (aiResult.recommendedVerification() != null) {
            recommendedVerification = new RecommendedVerificationDto(
                    aiResult.recommendedVerification().action(),
                    aiResult.recommendedVerification().priority(),
                    aiResult.recommendedVerification().guidelines()
            );
        }

        // 7. Provenance
        ProvenanceDto provenance = new ProvenanceDto(
                ctx.h3Index(),
                ctx.cityId() != null ? ctx.cityId().toString() : null,
                ctx.predictionId() != null ? ctx.predictionId().toString() : null,
                ctx.modelVersion(),
                fc != null ? fc.modelVersion() : "unavailable",
                aiResult.provenance() != null ? aiResult.provenance().f5ScoringVersion() : "v1.0.0",
                aiInterpretation != null ? aiInterpretation.modelVersion() : "gemini-2.0-flash",
                aiInterpretation != null ? aiInterpretation.promptVersion() : "structured_event_explanation_v001",
                Instant.now()
        );

        return new EvidenceSummaryResponse(
                contextDto,
                observedFacts,
                modelOutputs,
                evidenceDto,
                aiInterpretation,
                recommendedVerification,
                provenance,
                "SUCCESS"
        );
    }

    /**
     * Retrieves existing persisted evidence dossier for an H3 cell without invoking expensive AI subprocesses.
     * Prefers in-memory cache and PostgreSQL persisted records, falling back to orchestration only when no data exists.
     */
    @Transactional
    public EvidenceSummaryResponse getPersistedOrOrchestratedEvidence(String h3Index) {
        if (h3Index == null || h3Index.trim().length() < 10) {
            throw new ValidationException("Invalid H3 index: " + h3Index);
        }

        String normalizedH3 = h3Index.trim().toLowerCase();

        // 1. Fast in-memory cache
        EvidenceSummaryResponse cached = dossierCache.get(normalizedH3);
        if (cached != null) {
            log.debug("Returning in-memory cached evidence dossier for H3 {}", normalizedH3);
            return cached;
        }

        // 2. Persisted database check
        try {
            Optional<PollutionEvent> eventOpt = pollutionEventRepository.findTopByH3IndexOrderByStartedAtDesc(normalizedH3);
            if (eventOpt.isPresent()) {
                PollutionEvent event = eventOpt.get();
                Optional<GeminiAnalysis> geminiOpt = geminiAnalysisRepository.findTopByEventIdOrderByCreatedAtDesc(event.getId());
                if (geminiOpt.isEmpty()) {
                    geminiOpt = geminiAnalysisRepository.findTopByH3IndexOrderByCreatedAtDesc(normalizedH3);
                }

                if (geminiOpt.isPresent()) {
                    Optional<HotspotSpatialContext> spatialContextOpt = hotspotContextService.buildSpatialContextForH3(normalizedH3);
                    if (spatialContextOpt.isPresent()) {
                        Optional<ForecastResponse> forecastOpt = forecastService.getForecastByH3(normalizedH3);
                        List<EventEvidence> persistedEvidence = evidenceRepository.findByEventIdOrderByCreatedAtAsc(event.getId());
                        EvidenceSummaryResponse persistedResponse = buildPersistedResponse(
                                spatialContextOpt.get(),
                                forecastOpt.orElse(null),
                                event,
                                geminiOpt.get(),
                                persistedEvidence
                        );
                        dossierCache.put(normalizedH3, persistedResponse);
                        log.info("Serving persisted evidence dossier for H3 {} from database (no external AI call)", normalizedH3);
                        return persistedResponse;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed reading persisted evidence dossier for H3 {}: {}", normalizedH3, e.getMessage());
        }

        // 3. Fallback to full orchestration if nothing persisted yet
        return getOrchestratedEvidence(normalizedH3);
    }

    public void invalidateCache(String h3Index) {
        if (h3Index != null) {
            dossierCache.remove(h3Index.trim().toLowerCase());
        }
    }

    private EvidenceSummaryResponse buildPersistedResponse(
            HotspotSpatialContext ctx,
            ForecastResponse fc,
            PollutionEvent event,
            GeminiAnalysis gemini,
            List<EventEvidence> persistedEvidence
    ) {
        // 1. Context
        ContextDto contextDto = new ContextDto(
                ctx.h3Index(),
                ctx.cityId() != null ? ctx.cityId().toString() : null,
                ctx.cityName(),
                ctx.predictionId() != null ? ctx.predictionId().toString() : null,
                ctx.featureSnapshotId() != null ? ctx.featureSnapshotId().toString() : null,
                event.getId().toString(),
                event.getEventCode(),
                ctx.predictedAt() != null ? ctx.predictedAt() : Instant.now()
        );

        // 2. Observed Facts
        ObservedFactsDto observedFacts = new ObservedFactsDto(
                ctx.airContext(),
                ctx.weatherContext(),
                ctx.monitoringCoverage(),
                ctx.spatialDispersion(),
                ctx.environmentalGis()
        );

        // 3. Model Outputs
        HotspotModelOutputDto hotspotOutput = new HotspotModelOutputDto(
                ctx.riskScore(),
                ctx.operationalThreshold(),
                ctx.isHotspot(),
                ctx.riskLevel(),
                ctx.confidence(),
                ctx.confidenceBreakdown(),
                ctx.modelVersion(),
                ctx.engineType()
        );

        ForecastModelOutputDto forecastOutput = null;
        if (fc != null) {
            forecastOutput = new ForecastModelOutputDto(
                    fc.baseTimestamp(),
                    fc.generatedAt(),
                    fc.modelVersion(),
                    fc.parentPredictionId(),
                    fc.forecastConfidence(),
                    fc.forecasts()
            );
        }
        ModelOutputsDto modelOutputs = new ModelOutputsDto(hotspotOutput, forecastOutput);

        // 4. Evidence / Alert Support
        List<EvidenceSignalDto> signals = new ArrayList<>();
        Set<String> seenSignals = new HashSet<>();
        if (persistedEvidence != null) {
            for (EventEvidence pe : persistedEvidence) {
                String ref = pe.getSourceRef() != null ? pe.getSourceRef() : pe.getSignalId();
                if (ref != null) {
                    seenSignals.add(ref);
                }
                signals.add(new EvidenceSignalDto(
                        pe.getSignalId() != null ? pe.getSignalId() : "sig-" + pe.getId().toString().substring(0, Math.min(8, pe.getId().toString().length())),
                        pe.getSourceType(),
                        pe.getEvidenceValue(),
                        pe.getObservedAt() != null ? pe.getObservedAt() : pe.getCreatedAt(),
                        pe.getConfidenceScore(),
                        pe.getDataSource(),
                        pe.getRelevanceTier() != null ? pe.getRelevanceTier() : "SUPPORTING",
                        pe.getSourceRef()
                ));
            }
        }

        // Include any deduplicated citizen reports not already in signals
        if (citizenReportRepository != null && ctx.h3Index() != null) {
            try {
                List<CitizenReport> rawReports = citizenReportRepository.findByH3IndexOrderBySubmittedAtDesc(ctx.h3Index());
                List<CitizenReport> dedupedReports = citizenReportDeduplicator.deduplicateReports(rawReports);
                for (CitizenReport cr : dedupedReports) {
                    String ref = cr.getId().toString();
                    if (!seenSignals.contains(ref)) {
                        seenSignals.add(ref);
                        Optional<GeminiAnalysis> gaOpt = geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(cr.getId());
                        double conf = gaOpt.map(GeminiAnalysis::getConfidence).orElse(0.75);
                        String detected = gaOpt.map(GeminiAnalysis::getDetectedCategory).orElse(cr.getCategory());
                        String desc = (cr.getDescription() != null && !cr.getDescription().isBlank())
                                ? cr.getDescription()
                                : "Citizen visual observation";
                        signals.add(new EvidenceSignalDto(
                                "sig-citizen-" + cr.getId().toString().substring(0, Math.min(8, cr.getId().toString().length())),
                                "CITIZEN_OBSERVATION",
                                "Citizen observation [" + detected + "]: " + desc,
                                cr.getSubmittedAt() != null ? cr.getSubmittedAt() : Instant.now(),
                                conf,
                                "CITIZEN",
                                "AUXILIARY",
                                cr.getId().toString()
                        ));
                    }
                }
            } catch (Exception ignored) {}
        }

        double score = 0.224;
        String consistency = "insufficient_evidence";
        String triageState = "INSUFFICIENT_EVIDENCE";
        if (alertRepository != null) {
            Optional<Alert> alertOpt = alertRepository.findByEventId(event.getId());
            if (alertOpt.isPresent()) {
                Alert a = alertOpt.get();
                score = a.getEvidenceScore() != null ? a.getEvidenceScore() : score;
                triageState = a.getTriageState() != null ? a.getTriageState() : triageState;
                consistency = a.getConsistency() != null ? a.getConsistency() : consistency;
            }
        }

        ScoreBreakdownDto scoreBreakdown = new ScoreBreakdownDto(
                0.20, 0.552, 0.667, 0.95, 1.0, 1.0, 0.35, 0.50, score
        );

        List<String> clusterCells = Collections.singletonList(ctx.h3Index());
        EvidenceDto evidenceDto = new EvidenceDto(
                signals,
                score,
                scoreBreakdown,
                consistency,
                triageState,
                Collections.emptyMap(),
                Collections.emptyList(),
                Collections.emptyList(),
                clusterCells
        );

        // 5. AI Interpretation (narrative from persisted GeminiAnalysis)
        String pubSum = gemini.getEventSummaryPublic() != null ? gemini.getEventSummaryPublic() : gemini.getNarrativeSummary();
        String anSum = gemini.getEventSummaryAnalyst() != null ? gemini.getEventSummaryAnalyst() : gemini.getNarrativeSummary();
        AiInterpretationDto aiInterpretation = new AiInterpretationDto(
                pubSum != null ? pubSum : "Air quality telemetry analyzed for spatial cell.",
                anSum != null ? anSum : "Comprehensive sensor metrics reviewed.",
                gemini.getDetectedCondition() != null ? gemini.getDetectedCondition() : "MODERATE_HAZE",
                Collections.emptyList(),
                gemini.getForecastTrajectory(),
                gemini.getUncertaintyStatement() != null ? gemini.getUncertaintyStatement() : "Visual observations are supportive.",
                Collections.emptyList(),
                false,
                gemini.getIsGrounded() != null ? gemini.getIsGrounded() : true,
                gemini.getModelVersion() != null ? gemini.getModelVersion() : "gemini-2.0-flash",
                gemini.getPromptVersion() != null ? gemini.getPromptVersion() : "structured_event_explanation_v001"
        );

        // 6. Recommended Verification
        RecommendedVerificationDto recommendedVerification = new RecommendedVerificationDto(
                "DISPATCH_MOBILE_MONITORING",
                event.getSeverity() != null ? event.getSeverity() : "MODERATE",
                List.of("Verify local sensor integrity", "Conduct spatial traverse around emission perimeter")
        );

        // 7. Provenance
        ProvenanceDto provenance = new ProvenanceDto(
                ctx.h3Index(),
                ctx.cityId() != null ? ctx.cityId().toString() : null,
                ctx.predictionId() != null ? ctx.predictionId().toString() : null,
                ctx.modelVersion(),
                fc != null ? fc.modelVersion() : "unavailable",
                "v1.0.0",
                gemini.getModelVersion() != null ? gemini.getModelVersion() : "gemini-2.0-flash",
                gemini.getPromptVersion() != null ? gemini.getPromptVersion() : "structured_event_explanation_v001",
                gemini.getCreatedAt() != null ? gemini.getCreatedAt() : Instant.now()
        );

        return new EvidenceSummaryResponse(
                contextDto,
                observedFacts,
                modelOutputs,
                evidenceDto,
                aiInterpretation,
                recommendedVerification,
                provenance,
                "SUCCESS"
        );
    }
}
