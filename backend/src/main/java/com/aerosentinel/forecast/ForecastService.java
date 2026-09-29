package com.aerosentinel.forecast;

import com.aerosentinel.feature.FeatureSnapshot;
import com.aerosentinel.feature.FeatureSnapshotRepository;
import com.aerosentinel.forecast.feature.ForecastFeatureAdapter;
import com.aerosentinel.forecast.feature.ForecastFeatureAdapter.ModelReadyFeatureVector;
import com.aerosentinel.forecast.feature.ForecastFeatureBuilder;
import com.aerosentinel.forecast.feature.ForecastFeatureValidator;
import com.aerosentinel.forecast.feature.ForecastFeatureVector;
import com.aerosentinel.hotspot.HotspotPrediction;
import com.aerosentinel.hotspot.HotspotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Core Orchestrator for F4 Forecast Generation, Validation, Persistence, and Retrieval.
 *
 * Sequence:
 * 1. Resolve authoritative F3 parent hotspot prediction.
 * 2. Enforce strict spatial and city lineage.
 * 3. Retrieve or build P2 production ForecastFeatureVector.
 * 4. Validate vector schema, range, and quality.
 * 5. Adapt to model-ready (1, 36) vector.
 * 6. Invoke P3 Python CLI inference via ForecastAiClient.
 * 7. Transactionally persist all 3 horizon rows (1, 3, 6).
 * 8. Return canonical ForecastResponse.
 */
@Service
public class ForecastService {

    private static final Logger log = LoggerFactory.getLogger(ForecastService.class);

    private final ForecastRepository forecastRepository;
    private final HotspotRepository hotspotRepository;
    private final FeatureSnapshotRepository featureSnapshotRepository;
    private final ForecastFeatureBuilder featureBuilder;
    private final ForecastFeatureValidator featureValidator;
    private final ForecastFeatureAdapter featureAdapter;
    private final ForecastAiClient aiClient;

    public ForecastService(
            ForecastRepository forecastRepository,
            HotspotRepository hotspotRepository,
            FeatureSnapshotRepository featureSnapshotRepository,
            ForecastFeatureBuilder featureBuilder,
            ForecastFeatureValidator featureValidator,
            ForecastFeatureAdapter featureAdapter,
            ForecastAiClient aiClient
    ) {
        this.forecastRepository = forecastRepository;
        this.hotspotRepository = hotspotRepository;
        this.featureSnapshotRepository = featureSnapshotRepository;
        this.featureBuilder = featureBuilder;
        this.featureValidator = featureValidator;
        this.featureAdapter = featureAdapter;
        this.aiClient = aiClient;
    }

    /**
     * Retrieves the latest persisted forecast for a given H3 cell.
     *
     * @param h3Index 15-character Uber H3 cell index
     * @return Optional containing ForecastResponse if found, or empty if NO_DATA
     */
    @Transactional(readOnly = true)
    public Optional<ForecastResponse> getForecastByH3(String h3Index) {
        if (h3Index == null || h3Index.isBlank()) {
            throw new ForecastException.ValidationFailed("h3Index must not be null or blank");
        }

        List<Forecast> forecasts = forecastRepository.findLatestByH3Index(h3Index);
        if (forecasts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(ForecastMapper.toResponse(forecasts));
    }

    /**
     * Retrieves the persisted forecast for a specific F3 parent prediction.
     */
    @Transactional(readOnly = true)
    public Optional<ForecastResponse> getForecastByParentPredictionId(UUID parentPredictionId) {
        if (parentPredictionId == null) {
            throw new ForecastException.ValidationFailed("parentPredictionId must not be null");
        }

        List<Forecast> forecasts = forecastRepository.findByParentPredictionIdOrderByHorizonHoursAsc(parentPredictionId);
        if (forecasts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(ForecastMapper.toResponse(forecasts));
    }

    /**
     * Generates a multi-horizon forecast for an existing F3 parent hotspot prediction.
     * Enforces strict F3 lineage, P2 feature preparation, P3 AI execution, and atomic DB persistence.
     */
    @Transactional
    public ForecastResponse generateForecast(ForecastGenerateRequest request) {
        if (request == null || request.parentPredictionId() == null) {
            throw new ForecastException.ValidationFailed("parentPredictionId is required");
        }

        UUID parentId = request.parentPredictionId();

        // 1. Resolve authoritative F3 parent prediction first
        HotspotPrediction parent = hotspotRepository.findById(parentId)
                .orElseThrow(() -> new ForecastException.ParentNotFound("F3 Parent prediction not found: " + parentId));

        // Derive authoritative context from F3 parent record
        UUID authoritativeCityId = parent.getCityId();
        String authoritativeH3Index = parent.getH3Index();
        UUID authoritativeSnapshotId = parent.getFeatureSnapshotId();
        Instant authoritativePredictedAt = parent.getPredictedAt();

        // Lineage consistency validation against authoritative F3 parent record
        if (request.cityId() != null && !request.cityId().equals(authoritativeCityId)) {
            throw new ForecastException.ParentContextMismatch("Requested cityId " + request.cityId() +
                    " does not match authoritative F3 parent prediction cityId " + authoritativeCityId);
        }
        if (request.h3Index() != null && !request.h3Index().equalsIgnoreCase(authoritativeH3Index)) {
            throw new ForecastException.ParentContextMismatch("Requested h3Index " + request.h3Index() +
                    " does not match authoritative F3 parent prediction h3Index " + authoritativeH3Index);
        }

        log.info("Generating F4 forecast for parentPredictionId={}, cityId={}, h3Index={}, snapshotId={}, predictedAt={}",
                parentId, authoritativeCityId, authoritativeH3Index, authoritativeSnapshotId, authoritativePredictedAt);

        // 2. Build or resolve P2 Feature Vector using authoritative context
        ForecastFeatureVector featureVector;
        if (authoritativeSnapshotId != null) {
            Optional<FeatureSnapshot> snapshotOpt = featureSnapshotRepository.findById(authoritativeSnapshotId);
            if (snapshotOpt.isPresent()) {
                featureVector = featureBuilder.buildFromSnapshot(snapshotOpt.get());
            } else {
                log.warn("FeatureSnapshot {} referenced by parent not found in DB, generating from context", authoritativeSnapshotId);
                featureVector = featureBuilder.buildFromContext(authoritativeCityId, authoritativeH3Index, authoritativePredictedAt);
            }
        } else {
            featureVector = featureBuilder.buildFromContext(authoritativeCityId, authoritativeH3Index, authoritativePredictedAt);
        }

        // 3. Validate feature vector
        featureValidator.validate(featureVector);

        // 4. Adapt to model-ready (1, 36) shape
        ModelReadyFeatureVector modelReadyVector = featureAdapter.adapt(featureVector);

        // 5. Invoke Python P3 CLI process bridge with authoritative T0 baseTimestamp
        ForecastAiClient.ForecastInferenceResultDto inferenceResult = aiClient.predict(parentId, authoritativePredictedAt, modelReadyVector);

        // 6. Transactional persistence: Delete any existing forecasts for this parent prediction for idempotency
        if (forecastRepository.existsByParentPredictionId(parentId)) {
            log.info("Replacing existing forecast records for parentPredictionId={}", parentId);
            forecastRepository.deleteByParentPredictionId(parentId);
            forecastRepository.flush();
        }

        Instant generatedAt = Instant.parse(inferenceResult.generatedAt());
        String modelVersion = inferenceResult.modelVersion();
        UUID effectiveSnapshotId = modelReadyVector.featureSnapshotId() != null
                ? modelReadyVector.featureSnapshotId()
                : authoritativeSnapshotId;

        if (effectiveSnapshotId == null) {
            throw new ForecastException.ValidationFailed(
                    "Feature snapshot lineage required: featureSnapshotId cannot be null for SUCCESS forecast");
        }

        List<Forecast> entitiesToPersist = new ArrayList<>();
        for (ForecastAiClient.ForecastInferenceResultDto.ForecastItemDto item : inferenceResult.forecasts()) {
            Forecast forecast = new Forecast(
                    parentId,
                    authoritativeCityId,
                    authoritativeH3Index,
                    parent.getGridCellId(),
                    effectiveSnapshotId,
                    generatedAt,
                    Instant.parse(item.targetTime()),
                    item.horizonHours(),
                    item.predictedPm25(),
                    item.lowerBound(),
                    item.upperBound(),
                    null, // forecastConfidence strictly null
                    modelVersion,
                    item.unit(),
                    "SUCCESS"
            );
            entitiesToPersist.add(forecast);
        }

        List<Forecast> saved = forecastRepository.saveAll(entitiesToPersist);
        log.info("Successfully persisted {} forecast horizons for parentPredictionId={}", saved.size(), parentId);

        return ForecastMapper.toResponse(saved);
    }
}
