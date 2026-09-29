package com.aerosentinel.event;

import com.aerosentinel.alert.Alert;
import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.citizen.CitizenReport;
import com.aerosentinel.citizen.CitizenReportRepository;
import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.dto.alert.CitizenEvidenceItemDto;
import com.aerosentinel.dto.event.PollutionEventContextDto;
import com.aerosentinel.dto.event.PollutionEventContextDto.*;
import com.aerosentinel.dto.evidence.EvidenceSummaryResponse;
import com.aerosentinel.evidence.EventEvidence;
import com.aerosentinel.evidence.EvidenceOrchestrationService;
import com.aerosentinel.evidence.EvidenceRepository;
import com.aerosentinel.forecast.Forecast;
import com.aerosentinel.forecast.ForecastRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative Domain Service for Feature 7 Pollution Event Context.
 *
 * Prediction != Event != Alert != Action
 *
 * Implements strict domain context synthesis across:
 * - F3 Hotspot Prediction (riskScore, riskLevel, modelVersion)
 * - F4 Forecast (+1h, +3h, +6h trajectories)
 * - F5 Evidence & Triage (multi-source scoring, consistency, gating)
 * - F6 Citizen Evidence (ground observations, photos, Gemini Vision)
 * - F7 Alert (authority candidate if authoritative triage allows)
 */
@Service
public class PollutionEventService {

    private static final Logger log = LoggerFactory.getLogger(PollutionEventService.class);

    private final PollutionEventRepository eventRepository;
    private final HotspotRepository hotspotRepository;
    private final ForecastRepository forecastRepository;
    private final EvidenceRepository evidenceRepository;
    private final AlertRepository alertRepository;
    private final CitizenReportRepository citizenReportRepository;
    private final GeminiAnalysisRepository geminiAnalysisRepository;
    private final GridRepository gridRepository;
    private final CityRepository cityRepository;
    private final EvidenceOrchestrationService evidenceOrchestrationService;

    @Autowired
    public PollutionEventService(
            PollutionEventRepository eventRepository,
            @Autowired(required = false) HotspotRepository hotspotRepository,
            @Autowired(required = false) ForecastRepository forecastRepository,
            @Autowired(required = false) EvidenceRepository evidenceRepository,
            @Autowired(required = false) AlertRepository alertRepository,
            @Autowired(required = false) CitizenReportRepository citizenReportRepository,
            @Autowired(required = false) GeminiAnalysisRepository geminiAnalysisRepository,
            @Autowired(required = false) GridRepository gridRepository,
            @Autowired(required = false) CityRepository cityRepository,
            @Autowired(required = false) @Lazy EvidenceOrchestrationService evidenceOrchestrationService
    ) {
        this.eventRepository = eventRepository;
        this.hotspotRepository = hotspotRepository;
        this.forecastRepository = forecastRepository;
        this.evidenceRepository = evidenceRepository;
        this.alertRepository = alertRepository;
        this.citizenReportRepository = citizenReportRepository;
        this.geminiAnalysisRepository = geminiAnalysisRepository;
        this.gridRepository = gridRepository;
        this.cityRepository = cityRepository;
        this.evidenceOrchestrationService = evidenceOrchestrationService;
    }

    @Transactional(readOnly = true)
    public List<PollutionEventContextDto> getAllEventContexts() {
        return eventRepository.findAllByOrderByStartedAtDesc().stream()
                .map(this::toContextDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PollutionEventContextDto> getEventContextById(UUID id) {
        return eventRepository.findById(id).map(this::toContextDto);
    }

    @Transactional(readOnly = true)
    public List<PollutionEventContextDto> getEventsByH3(String h3Index) {
        return eventRepository.findByH3IndexOrderByStartedAtDesc(h3Index).stream()
                .map(this::toContextDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PollutionEventContextDto> getEventsByCity(UUID cityId) {
        if (cityId == null || gridRepository == null) {
            return getAllEventContexts();
        }
        return eventRepository.findAllByOrderByStartedAtDesc().stream()
                .filter(e -> {
                    if (e.getGridCellId() != null) {
                        return gridRepository.findById(e.getGridCellId())
                                .map(g -> cityId.equals(g.getCityId()))
                                .orElse(false);
                    }
                    return false;
                })
                .map(this::toContextDto)
                .collect(Collectors.toList());
    }

    // Legacy method signatures for backward compatibility
    @Transactional(readOnly = true)
    public List<PollutionEventContextDto> getAllEvents() {
        return getAllEventContexts();
    }

    @Transactional(readOnly = true)
    public Optional<PollutionEventContextDto> getEventById(UUID id) {
        return getEventContextById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PollutionEvent> getEventEntityById(UUID id) {
        return eventRepository.findById(id);
    }

    @Transactional
    public PollutionEvent saveEvent(PollutionEvent event) {
        return eventRepository.save(event);
    }

    /**
     * Maps an authoritative PollutionEvent into the unified multi-source PollutionEventContextDto.
     * Enforces strict metric separation and avoids recomputation of F3/F4/F5/F6 metrics.
     */
    public PollutionEventContextDto toContextDto(PollutionEvent event) {
        if (event == null) return null;

        // 1. Resolve Grid & City info
        UUID cityId = null;
        String cityName = null;
        if (event.getGridCellId() != null && gridRepository != null) {
            Optional<GridCell> cellOpt = gridRepository.findById(event.getGridCellId());
            if (cellOpt.isPresent()) {
                cityId = cellOpt.get().getCityId();
                if (cityId != null && cityRepository != null) {
                    cityName = cityRepository.findById(cityId).map(City::getName).orElse(null);
                }
            }
        }

        // 2. F3 Hotspot Prediction Context (Strict Lineage Preservation)
        PredictionContextDto predDto = null;
        HotspotPrediction pred = null;
        if (event.getPredictionId() != null && hotspotRepository != null) {
            pred = hotspotRepository.findById(event.getPredictionId()).orElse(null);
        }
        if (pred == null && event.getH3Index() != null && hotspotRepository != null) {
            pred = hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(event.getH3Index()).orElse(null);
        }
        if (pred != null) {
            if (cityId == null) cityId = pred.getCityId();
            predDto = new PredictionContextDto(
                    pred.getId(),
                    pred.getH3Index(),
                    pred.getRiskScore(),
                    pred.getRiskLevel(),
                    pred.getConfidence(),
                    0.20,
                    pred.getModelVersion() != null ? pred.getModelVersion() : "hotspot_classifier_v1",
                    pred.getPredictedAt()
            );
        }

        // 3. F4 Multi-Horizon Forecast Context (+1h, +3h, +6h)
        ForecastContextDto fcDto = null;
        List<Forecast> forecasts = Collections.emptyList();
        if (event.getPredictionId() != null && forecastRepository != null) {
            forecasts = forecastRepository.findByParentPredictionIdOrderByHorizonHoursAsc(event.getPredictionId());
        }
        if (forecasts.isEmpty() && event.getH3Index() != null && forecastRepository != null) {
            forecasts = forecastRepository.findLatestByH3Index(event.getH3Index());
        }

        if (!forecasts.isEmpty()) {
            Forecast f0 = forecasts.get(0);
            Instant baseTime = f0.getGeneratedAt();
            if (pred != null && pred.getPredictedAt() != null) {
                baseTime = pred.getPredictedAt();
            }
            List<ForecastContextDto.HorizonDto> horizons = forecasts.stream()
                    .map(f -> new ForecastContextDto.HorizonDto(
                            f.getHorizonHours(),
                            f.getTargetTime(),
                            f.getPredictedPm25(),
                            f.getLowerBound() != null ? f.getLowerBound() : f.getPredictedPm25() * 0.9,
                            f.getUpperBound() != null ? f.getUpperBound() : f.getPredictedPm25() * 1.1,
                            f.getUnit() != null ? f.getUnit() : "ug/m3"
                    ))
                    .collect(Collectors.toList());
            fcDto = new ForecastContextDto(
                    true,
                    "AVAILABLE",
                    f0.getParentPredictionId(),
                    f0.getModelVersion() != null ? f0.getModelVersion() : "forecast_regressors_v1",
                    baseTime,
                    f0.getGeneratedAt(),
                    f0.getForecastConfidence(),
                    horizons
            );
        } else {
            fcDto = new ForecastContextDto(
                    false,
                    "UNAVAILABLE",
                    event.getPredictionId(),
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList()
            );
        }

        // 4. F5 Evidence & Alert Resolution
        List<EventEvidence> evidenceRows = Collections.emptyList();
        if (evidenceRepository != null) {
            evidenceRows = evidenceRepository.findByEventIdOrderByCreatedAtAsc(event.getId());
        }

        Alert alert = null;
        if (alertRepository != null) {
            alert = alertRepository.findByEventId(event.getId()).orElse(null);
        }

        Double evidenceScore = alert != null ? alert.getEvidenceScore() : null;
        String consistency = alert != null ? alert.getConsistency() : null;
        String triageState = alert != null ? alert.getTriageState() : null;
        Double completeness = null;

        // If no alert exists, check if there's a cached or persisted evidence dossier for this H3
        if (evidenceScore == null && evidenceOrchestrationService != null && event.getH3Index() != null) {
            try {
                EvidenceSummaryResponse dossier = evidenceOrchestrationService.getPersistedOrOrchestratedEvidence(event.getH3Index());
                if (dossier != null && dossier.evidence() != null) {
                    evidenceScore = dossier.evidence().evidenceScore();
                    consistency = dossier.evidence().consistency();
                    triageState = dossier.evidence().triageState();
                    if (dossier.evidence().scoreBreakdown() != null) {
                        completeness = dossier.evidence().scoreBreakdown().evidenceCompleteness();
                    }
                }
            } catch (Exception ex) {
                log.debug("No evidence dossier available for event {} H3 {}: {}", event.getId(), event.getH3Index(), ex.getMessage());
            }
        }

        // Signals mapping
        List<EvidenceContextDto.SignalSummaryDto> signalSummaries = new ArrayList<>();
        for (EventEvidence ev : evidenceRows) {
            signalSummaries.add(new EvidenceContextDto.SignalSummaryDto(
                    ev.getSignalId() != null ? ev.getSignalId() : "sig-" + ev.getId().toString().substring(0, Math.min(8, ev.getId().toString().length())),
                    ev.getSourceType(),
                    ev.getDataSource(),
                    ev.getRelevanceTier(),
                    ev.getEvidenceValue(),
                    ev.getConfidenceScore(),
                    ev.getObservedAt()
            ));
        }

        EvidenceContextDto evDto = new EvidenceContextDto(
                evidenceScore,
                consistency,
                triageState != null ? triageState : "UNEVALUATED",
                completeness,
                signalSummaries.size(),
                signalSummaries
        );

        // 5. F6 Citizen Evidence (Preserve AUXILIARY, safe photo URLs, Gemini Vision observations)
        List<CitizenEvidenceItemDto> citizenEvidence = resolveCitizenEvidence(event, evidenceRows);

        // 6. F7 Alert Context (Alert != Event)
        AlertContextDto alertDto = null;
        if (alert != null) {
            alertDto = new AlertContextDto(
                    true,
                    alert.getId(),
                    alert.getStatus(),
                    alert.getSeverity(),
                    alert.getTriageState(),
                    alert.getTitle(),
                    alert.getMessage(),
                    alert.getAcknowledgedAt(),
                    alert.getResolvedAt(),
                    null,
                    null
            );
        } else {
            alertDto = new AlertContextDto(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        EventSummaryDto eventSummary = new EventSummaryDto(
                event.getId(),
                event.getEventCode(),
                event.getGridCellId(),
                event.getH3Index(),
                event.getPredictionId(),
                event.getSeverity(),
                event.getStatus(),
                event.getStartedAt(),
                event.getResolvedAt(),
                event.getCreatedAt()
        );

        return new PollutionEventContextDto(
                event.getId(),
                event.getEventCode(),
                event.getGridCellId(),
                event.getH3Index(),
                cityId,
                cityName,
                event.getPredictionId(),
                event.getSeverity(),
                event.getStatus(),
                event.getStartedAt(),
                event.getResolvedAt(),
                event.getCreatedAt(),
                eventSummary,
                predDto,
                fcDto,
                evDto,
                citizenEvidence,
                alertDto
        );
    }

    private List<CitizenEvidenceItemDto> resolveCitizenEvidence(PollutionEvent event, List<EventEvidence> evidenceRows) {
        List<CitizenEvidenceItemDto> citizenList = new ArrayList<>();
        if (evidenceRows == null || evidenceRows.isEmpty()) {
            return citizenList;
        }

        for (EventEvidence ev : evidenceRows) {
            if ("CITIZEN".equalsIgnoreCase(ev.getDataSource()) || "CITIZEN_OBSERVATION".equalsIgnoreCase(ev.getSourceType())) {
                UUID repId = null;
                try {
                    if (ev.getSourceRef() != null) {
                        repId = UUID.fromString(ev.getSourceRef().trim());
                    }
                } catch (Exception ignored) {}

                CitizenReport report = null;
                if (repId != null && citizenReportRepository != null) {
                    report = citizenReportRepository.findById(repId).orElse(null);
                }

                GeminiAnalysis ga = null;
                if (repId != null && geminiAnalysisRepository != null) {
                    ga = geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(repId).orElse(null);
                }

                String photoUrl = null;
                if (report != null && report.getImageUrl() != null && !report.getImageUrl().isBlank()) {
                    String raw = report.getImageUrl().trim();
                    if (raw.startsWith("/api/v1/citizen/photos/")) {
                        photoUrl = raw;
                    } else {
                        String filename = raw.replace('\\', '/');
                        int slashIdx = filename.lastIndexOf('/');
                        if (slashIdx >= 0) {
                            filename = filename.substring(slashIdx + 1);
                        }
                        photoUrl = "/api/v1/citizen/photos/" + filename;
                    }
                }

                String repRef = "CR-" + (repId != null ? repId.toString().substring(0, Math.min(8, repId.toString().length())).toUpperCase() : "UNKNOWN");
                String category = report != null ? report.getCategory() : "CITIZEN_OBSERVATION";
                String description = report != null ? report.getDescription() : ev.getEvidenceValue();
                Instant obsAt = report != null && report.getSubmittedAt() != null ? report.getSubmittedAt() : ev.getObservedAt();
                String visibleCond = ga != null ? ga.getDetectedCategory() : (report != null ? report.getCategory() : "UNKNOWN");
                Double visConf = ga != null && ga.getConfidence() != null ? ga.getConfidence() : ev.getConfidenceScore();
                List<String> obs = (ga != null && ga.getNarrativeSummary() != null && !ga.getNarrativeSummary().isBlank())
                        ? List.of(ga.getNarrativeSummary())
                        : Collections.emptyList();
                List<String> uncert = (ga != null && ga.getUncertaintyStatement() != null && !ga.getUncertaintyStatement().isBlank())
                        ? List.of(ga.getUncertaintyStatement())
                        : Collections.emptyList();

                citizenList.add(new CitizenEvidenceItemDto(
                        repId,
                        repRef,
                        event.getH3Index(),
                        category,
                        description,
                        obsAt,
                        visibleCond,
                        visConf,
                        obs,
                        uncert,
                        photoUrl,
                        "CITIZEN",
                        "AUXILIARY",
                        event.getId(),
                        ev.getEvidenceKey()
                ));
            }
        }
        return citizenList;
    }
}
