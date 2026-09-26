package com.aerosentinel.hotspot;

import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.feature.FeatureEngineeringService;
import com.aerosentinel.feature.FeatureRecord;
import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import com.aerosentinel.grid.GridCell;
import com.aerosentinel.grid.GridRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class HotspotService {

    private static final Logger log = LoggerFactory.getLogger(HotspotService.class);

    private final HotspotRepository hotspotRepository;
    private final GridRepository gridRepository;
    private final CityRepository cityRepository;
    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final FeatureEngineeringService featureEngineeringService;
    private final HotspotDetectionEngine mlDetectionEngine;
    private final HotspotDetectionEngine baselineDetectionEngine;
    private final HotspotPredictionValidator validator;
    private final HotspotContextService contextService;

    public static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Autowired
    public HotspotService(
            HotspotRepository hotspotRepository,
            GridRepository gridRepository,
            CityRepository cityRepository,
            FeatureSnapshotRepository featureSnapshotRepository,
            FeatureEngineeringService featureEngineeringService,
            @Qualifier("mlHotspotDetectionEngine") HotspotDetectionEngine mlDetectionEngine,
            @Qualifier("baselineHotspotDetectionEngine") HotspotDetectionEngine baselineDetectionEngine,
            HotspotPredictionValidator validator,
            @Autowired(required = false) HotspotContextService contextService
    ) {
        this.hotspotRepository = hotspotRepository;
        this.gridRepository = gridRepository;
        this.cityRepository = cityRepository;
        this.featureSnapshotRepository = featureSnapshotRepository;
        this.featureEngineeringService = featureEngineeringService;
        this.mlDetectionEngine = mlDetectionEngine;
        this.baselineDetectionEngine = baselineDetectionEngine;
        this.validator = validator;
        this.contextService = contextService;
    }

    public HotspotService(
            HotspotRepository hotspotRepository,
            GridRepository gridRepository,
            CityRepository cityRepository,
            FeatureSnapshotRepository featureSnapshotRepository,
            FeatureEngineeringService featureEngineeringService,
            HotspotDetectionEngine mlDetectionEngine,
            HotspotDetectionEngine baselineDetectionEngine,
            HotspotPredictionValidator validator
    ) {
        this(hotspotRepository, gridRepository, cityRepository, featureSnapshotRepository,
                featureEngineeringService, mlDetectionEngine, baselineDetectionEngine, validator, null);
    }

    /**
     * Resolves the appropriate detection engine based on model geography.
     * hotspot_classifier_v1 is trained exclusively for Pune PMR; non-Pune cities use baseline.
     */
    public HotspotDetectionEngine getEngineForCity(UUID cityId) {
        if (PUNE_CITY_ID.equals(cityId)) {
            return mlDetectionEngine;
        }
        return baselineDetectionEngine;
    }

    /**
     * Retrieves or computes the latest hotspot predictions for all H3 cells in the specified city.
     *
     * @param cityId unique city identifier
     * @return HotspotOverviewResponse containing clean cell-level predictions and aggregate freshness
     */
    @Transactional
    public HotspotOverviewResponse getHotspotsForCity(UUID cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found for id: " + cityId));

        HotspotDetectionEngine activeEngine = getEngineForCity(cityId);

        List<GridCell> cells = gridRepository.findByCityId(cityId);
        if (cells.isEmpty()) {
            return new HotspotOverviewResponse(
                    cityId,
                    city.getName(),
                    Instant.now(),
                    activeEngine.getEngineVersion(),
                    activeEngine.getEngineType(),
                    "NO_DATA",
                    0,
                    0,
                    Collections.emptyList()
            );
        }

        // Query latest predictions from database
        List<HotspotPrediction> latestPredictions = hotspotRepository.findLatestByCityId(cityId);

        // If predictions are missing, empty, or outdated relative to the active engine, generate them
        boolean needsGeneration = latestPredictions.isEmpty()
                || latestPredictions.size() < cells.size()
                || !activeEngine.getEngineVersion().equals(latestPredictions.get(0).getModelVersion());

        if (needsGeneration) {
            log.info("Generating {} hotspot predictions for city {} (cells={})",
                    activeEngine.getEngineType(), city.getName(), cells.size());
            List<HotspotPrediction> generated = generatePredictionsForCity(city, cells, activeEngine);
            if (!generated.isEmpty()) {
                latestPredictions = hotspotRepository.findLatestByCityId(cityId);
            }
        }

        List<HotspotCellDto> dtoList = new ArrayList<>();
        int highRiskCount = 0;
        Instant now = Instant.now();
        Instant mostRecentTime = null;

        for (HotspotPrediction pred : latestPredictions) {
            String freshness = computeFreshness(pred.getPredictedAt(), now);
            if (mostRecentTime == null || pred.getPredictedAt().isAfter(mostRecentTime)) {
                mostRecentTime = pred.getPredictedAt();
            }

            if ("HIGH".equalsIgnoreCase(pred.getRiskLevel()) || "CRITICAL".equalsIgnoreCase(pred.getRiskLevel())) {
                highRiskCount++;
            }

            String cellEngineType = "hotspot_classifier_v1".equalsIgnoreCase(pred.getModelVersion()) ? "ML" : "BASELINE";

            dtoList.add(new HotspotCellDto(
                    pred.getH3Index(),
                    pred.getGridCellId(),
                    pred.getRiskScore(),
                    pred.getRiskLevel(),
                    pred.getConfidence(),
                    pred.getPredictedAt(),
                    freshness,
                    pred.getModelVersion(),
                    pred.getId(),
                    pred.getCityId(),
                    city.getName(),
                    cellEngineType,
                    pred.getFeatureSnapshotId(),
                    null
            ));
        }

        String overallFreshness = computeFreshness(mostRecentTime, now);

        return new HotspotOverviewResponse(
                cityId,
                city.getName(),
                mostRecentTime != null ? mostRecentTime : now,
                activeEngine.getEngineVersion(),
                activeEngine.getEngineType(),
                overallFreshness,
                dtoList.size(),
                highRiskCount,
                dtoList
        );
    }

    /**
     * Retrieves the latest prediction and rich spatial context for a single H3 cell.
     *
     * @param h3Index 15-character Uber H3 cell index
     * @return Optional HotspotCellDto with full spatial context populated
     */
    @Transactional
    public Optional<HotspotCellDto> getHotspotByH3(String h3Index) {
        Optional<HotspotPrediction> opt = hotspotRepository.findTopByH3IndexOrderByPredictedAtDesc(h3Index);
        if (opt.isPresent()) {
            HotspotPrediction pred = opt.get();
            String freshness = computeFreshness(pred.getPredictedAt(), Instant.now());
            String engineType = "hotspot_classifier_v1".equalsIgnoreCase(pred.getModelVersion()) ? "ML" : "BASELINE";
            String cityName = (pred.getCityId() != null)
                    ? cityRepository.findById(pred.getCityId()).map(City::getName).orElse("Pune")
                    : "Pune";
            HotspotSpatialContext context = (contextService != null)
                    ? contextService.buildSpatialContext(pred)
                    : null;

            return Optional.of(new HotspotCellDto(
                    pred.getH3Index(),
                    pred.getGridCellId(),
                    pred.getRiskScore(),
                    pred.getRiskLevel(),
                    pred.getConfidence(),
                    pred.getPredictedAt(),
                    freshness,
                    pred.getModelVersion(),
                    pred.getId(),
                    pred.getCityId(),
                    cityName,
                    engineType,
                    pred.getFeatureSnapshotId(),
                    context
            ));
        }

        // On-demand generation if cell exists in grid
        Optional<GridCell> cellOpt = gridRepository.findByH3Index(h3Index);
        if (cellOpt.isPresent()) {
            GridCell cell = cellOpt.get();
            HotspotDetectionEngine engine = getEngineForCity(cell.getCityId());
            try {
                HotspotPrediction pred = generatePredictionForCell(cell, Instant.now(), engine);
                String freshness = computeFreshness(pred.getPredictedAt(), Instant.now());
                String engineType = "hotspot_classifier_v1".equalsIgnoreCase(pred.getModelVersion()) ? "ML" : "BASELINE";
                String cityName = (cell.getCityId() != null)
                        ? cityRepository.findById(cell.getCityId()).map(City::getName).orElse("Pune")
                        : "Pune";
                HotspotSpatialContext context = (contextService != null)
                        ? contextService.buildSpatialContext(pred)
                        : null;

                return Optional.of(new HotspotCellDto(
                        pred.getH3Index(),
                        pred.getGridCellId(),
                        pred.getRiskScore(),
                        pred.getRiskLevel(),
                        pred.getConfidence(),
                        pred.getPredictedAt(),
                        freshness,
                        pred.getModelVersion(),
                        pred.getId(),
                        pred.getCityId(),
                        cityName,
                        engineType,
                        pred.getFeatureSnapshotId(),
                        context
                ));
            } catch (Exception e) {
                log.warn("On-demand prediction generation failed for cell {}: {}", h3Index, e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Directly retrieves the rich HotspotSpatialContext for a cell.
     */
    @Transactional(readOnly = true)
    public Optional<HotspotSpatialContext> getSpatialContextForH3(String h3Index) {
        if (contextService != null) {
            return contextService.buildSpatialContextForH3(h3Index);
        }
        return Optional.empty();
    }

    /**
     * Generates and persists predictions for all cells in a city using the specified engine.
     */
    private List<HotspotPrediction> generatePredictionsForCity(City city, List<GridCell> cells, HotspotDetectionEngine engine) {
        Instant now = Instant.now();
        List<HotspotPrediction> results = new ArrayList<>();

        for (GridCell cell : cells) {
            try {
                HotspotPrediction pred = generatePredictionForCell(cell, now, engine);
                results.add(pred);
            } catch (Exception e) {
                log.warn("Could not generate hotspot prediction for cell {}: {}", cell.getH3Index(), e.getMessage());
            }
        }
        return results;
    }

    /**
     * Generates, validates, and persists a single hotspot prediction from FeatureSnapshot.
     */
    public HotspotPrediction generatePredictionForCell(GridCell cell, Instant obsTime) {
        return generatePredictionForCell(cell, obsTime, getEngineForCity(cell.getCityId()));
    }

    /**
     * Generates, validates, and persists a single hotspot prediction from FeatureSnapshot with explicit engine.
     */
    public HotspotPrediction generatePredictionForCell(GridCell cell, Instant obsTime, HotspotDetectionEngine engine) {
        // 1. Retrieve or generate FeatureSnapshot
        FeatureSnapshot snapshot = featureSnapshotRepository
                .findTopByH3IndexOrderByObservedAtDesc(cell.getH3Index())
                .orElseGet(() -> {
                    FeatureRecord record = featureEngineeringService.generateFeatureRecord(cell.getCityId(), cell.getH3Index(), obsTime);
                    return featureSnapshotRepository
                            .findByH3IndexAndObservedAtAndFeatureSchemaVersion(cell.getH3Index(), obsTime, FeatureRecord.SCHEMA_VERSION)
                            .orElseThrow(() -> new IllegalStateException("Snapshot not found after generation"));
                });

        // 2. Evaluate through detection engine
        HotspotPredictionResult evalResult = engine.evaluate(snapshot);

        // 3. Validate prediction
        validator.validate(
                cell.getCityId(),
                cell.getH3Index(),
                snapshot.getObservedAt(),
                evalResult.riskScore(),
                evalResult.riskLevel().name(),
                evalResult.confidence(),
                evalResult.modelVersion(),
                snapshot.getId()
        );

        // 4. Build and persist prediction entity (history preserved)
        HotspotPrediction pred = new HotspotPrediction();
        pred.setGridCellId(cell.getId());
        pred.setCityId(cell.getCityId());
        pred.setH3Index(cell.getH3Index());
        pred.setPredictedAt(snapshot.getObservedAt());
        pred.setRiskScore(evalResult.riskScore());
        pred.setRiskLevel(evalResult.riskLevel().name());
        pred.setConfidence(evalResult.confidence());
        pred.setModelVersion(evalResult.modelVersion());
        pred.setFeatureSnapshotId(snapshot.getId());
        pred.setExplanationStatus("PENDING");
        pred.setCreatedAt(Instant.now());

        return hotspotRepository.save(pred);
    }

    private static String computeFreshness(Instant predictedAt, Instant now) {
        if (predictedAt == null) {
            return "NO_DATA";
        }
        Duration age = Duration.between(predictedAt, now);
        if (age.isNegative() || age.toHours() <= 2) {
            return "LIVE";
        } else if (age.toHours() <= 24) {
            return "STALE";
        } else {
            return "UNAVAILABLE";
        }
    }
}
