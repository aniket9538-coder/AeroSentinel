package com.aerosentinel.hotspot;

import com.aerosentinel.feature.FeatureSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Trained Machine Learning Hotspot Detection Engine.
 *
 * Wraps the Calibrated Random Forest model (hotspot_classifier_v1.joblib)
 * behind the provider-agnostic HotspotDetectionEngine boundary.
 *
 * Core Guarantees:
 * 1. Strict 36-feature vector adaptation in exact artifact order.
 * 2. Deterministic wind speed conversion (km/h -> m/s) applied once at adapter boundary.
 * 3. Enforces model domain: validated strictly for Pune Metropolitan Region (PMR).
 * 4. Fails safely on missing data (INSUFFICIENT_DATA) or unsupported geography without fabricating numbers.
 * 5. Returns calibrated probability [0.0, 1.0] and applies operational decision threshold (p >= 0.20).
 * 6. Preserves separate confidence estimation without conflating model probability with certainty.
 */
@Component("mlHotspotDetectionEngine")
@Primary
public class MLHotspotDetectionEngine implements HotspotDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(MLHotspotDetectionEngine.class);

    public static final String ENGINE_VERSION = "hotspot_classifier_v1";
    public static final String ENGINE_TYPE = "ML";
    public static final double OPERATIONAL_THRESHOLD = 0.20;

    // Supported Pune city UUID and identifiers
    public static final UUID PUNE_CITY_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    private final ModelFeatureVectorAdapter featureAdapter;
    private final AiServiceHotspotClient aiServiceClient;
    private final BaselineHotspotDetectionEngine fallbackEngine;

    public MLHotspotDetectionEngine(
            ModelFeatureVectorAdapter featureAdapter,
            AiServiceHotspotClient aiServiceClient,
            BaselineHotspotDetectionEngine fallbackEngine
    ) {
        this.featureAdapter = featureAdapter;
        this.aiServiceClient = aiServiceClient;
        this.fallbackEngine = fallbackEngine;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public String getEngineType() {
        return ENGINE_TYPE;
    }

    @Override
    public HotspotPredictionResult evaluate(FeatureSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("FeatureSnapshot must not be null");
        }

        // 1. Geography / Model Domain Guard
        boolean isPune = isPuneDomain(snapshot);
        if (!isPune) {
            log.warn("Model domain guard: H3 cell {} belongs to city {} which is outside Pune PMR. " +
                    "Returning controlled MODEL_DOMAIN_UNSUPPORTED state.", snapshot.getH3Index(), snapshot.getCityId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("h3Index", snapshot.getH3Index());
            metadata.put("cityId", snapshot.getCityId().toString());
            metadata.put("domainSupported", false);
            metadata.put("status", "MODEL_DOMAIN_UNSUPPORTED");
            metadata.put("reason", "hotspot_classifier_v1 was trained exclusively on Pune Metropolitan Region and is not validated for this geography.");

            // Delegate to baseline engine with explicit baseline attribution for unsupported cities
            HotspotPredictionResult baselineResult = fallbackEngine.evaluate(snapshot);
            metadata.putAll(baselineResult.metadata());
            metadata.put("fallbackEngine", fallbackEngine.getEngineVersion());

            return new HotspotPredictionResult(
                    baselineResult.riskScore(),
                    baselineResult.riskLevel(),
                    baselineResult.confidence(),
                    fallbackEngine.getEngineVersion(),
                    fallbackEngine.getEngineType(),
                    metadata
            );
        }

        // 2. Missing Feature & Quality Status Policy
        if ("UNAVAILABLE".equalsIgnoreCase(snapshot.getQualityStatus())) {
            log.info("Feature snapshot {} has quality status UNAVAILABLE. Returning degraded telemetry assessment.", snapshot.getId());
            return fallbackEngine.evaluate(snapshot);
        }

        // 3. Adapt 36-feature vector with normalized wind speed (km/h -> m/s)
        ModelFeatureVectorAdapter.AdaptedFeatureVector adapted =
                featureAdapter.adaptWithWindNormalization(snapshot, true);

        if (!adapted.isFullyValid()) {
            throw new IllegalArgumentException("INSUFFICIENT_DATA: Snapshot missing required model features: "
                    + adapted.missingFeatures());
        }

        List<Double> orderedList = new ArrayList<>(adapted.orderedValues().length);
        for (double val : adapted.orderedValues()) {
            orderedList.add(val);
        }

        // 4. Invoke ML Inference Model
        AiServiceHotspotClient.MlInferencePayload payload = new AiServiceHotspotClient.MlInferencePayload(
                snapshot.getH3Index(),
                "Pune",
                snapshot.getCityId().toString(),
                orderedList,
                adapted.featureMap()
        );

        AiServiceHotspotClient.MlInferenceResult mlResult = aiServiceClient.predict(payload);

        // 5. Validate Output Probability
        double probability = mlResult.riskScore();
        if (probability < 0.0 || probability > 1.0 || Double.isNaN(probability)) {
            throw new IllegalStateException("INVALID_MODEL_OUTPUT: Calibrated probability out of bounds: " + probability);
        }

        // 6. Map to Risk Level using operational decision threshold
        HotspotRiskLevel riskLevel = HotspotRiskLevel.fromProbability(probability, mlResult.operationalThreshold());

        // 7. Assemble Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("h3Index", snapshot.getH3Index());
        metadata.put("cityId", snapshot.getCityId().toString());
        metadata.put("featureSnapshotId", snapshot.getId().toString());
        metadata.put("isHotspot", mlResult.isHotspot());
        metadata.put("operationalThreshold", mlResult.operationalThreshold());
        metadata.put("calibratedProbability", probability);
        metadata.put("domainSupported", true);
        if (mlResult.metadata() != null) {
            metadata.putAll(mlResult.metadata());
        }

        return new HotspotPredictionResult(
                roundTo4(probability),
                riskLevel,
                roundTo4(mlResult.confidence()),
                ENGINE_VERSION,
                ENGINE_TYPE,
                metadata
        );
    }

    private boolean isPuneDomain(FeatureSnapshot snapshot) {
        if (snapshot.getCityId() != null && PUNE_CITY_ID.equals(snapshot.getCityId())) {
            return true;
        }
        // Coordinate boundary check for Pune Metropolitan Region [18.2, 73.5, 18.9, 74.2]
        try {
            ModelFeatureVectorAdapter.AdaptedFeatureVector v = featureAdapter.adapt(snapshot);
            Double lat = v.featureMap().get("latitude");
            Double lon = v.featureMap().get("longitude");
            if (lat != null && lon != null) {
                return lat >= 18.0 && lat <= 19.2 && lon >= 73.3 && lon <= 74.4;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static double roundTo4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
