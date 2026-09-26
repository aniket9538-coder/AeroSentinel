package com.aerosentinel.hotspot;

import com.aerosentinel.feature.FeatureSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Deterministic Product-Side Baseline Hotspot Detection Engine.
 *
 * Clearly marked engineType = "BASELINE".
 *
 * Rules:
 * 1. Fully deterministic multi-criteria atmospheric and pollutant aggregation.
 * 2. Zero random numbers, zero synthetic or fabricated values.
 * 3. Incorporates particulate burden, co-pollutants, atmospheric stagnation, fire decay, and spatial proximity.
 * 4. Adjusts confidence based on monitoring coverage gaps and sensor availability.
 * 5. Returns controlled degraded confidence when telemetry is UNAVAILABLE or MISSING.
 */
@Component("baselineHotspotDetectionEngine")
public class BaselineHotspotDetectionEngine implements HotspotDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(BaselineHotspotDetectionEngine.class);

    public static final String ENGINE_VERSION = "hotspot-baseline-v1";
    public static final String ENGINE_TYPE = "BASELINE";

    private final ModelFeatureVectorAdapter featureAdapter;

    public BaselineHotspotDetectionEngine(ModelFeatureVectorAdapter featureAdapter) {
        this.featureAdapter = featureAdapter;
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

        ModelFeatureVectorAdapter.AdaptedFeatureVector vector = featureAdapter.adapt(snapshot);
        Map<String, Double> f = vector.featureMap();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("h3Index", snapshot.getH3Index());
        metadata.put("featureSchemaVersion", snapshot.getFeatureSchemaVersion());
        metadata.put("qualityStatus", snapshot.getQualityStatus());
        metadata.put("missingFeatures", vector.missingFeatures());

        // Handle UNAVAILABLE / MISSING sensor telemetry
        if (!vector.isFullyValid() || "UNAVAILABLE".equalsIgnoreCase(snapshot.getQualityStatus())) {
            double lagVal = f.getOrDefault("pm25_spatial_lag_mean", 0.0);
            double baseRisk = Math.min(1.0, Math.max(0.0, lagVal / 100.0));
            double lowConfidence = 0.35; // Explicitly indicates low confidence due to missing sensors

            metadata.put("evaluationMode", "DEGRADED_SPATIAL_FALLBACK");
            return new HotspotPredictionResult(
                    roundTo4(baseRisk),
                    HotspotRiskLevel.fromScore(baseRisk),
                    lowConfidence,
                    ENGINE_VERSION,
                    ENGINE_TYPE,
                    metadata
            );
        }

        // 1. Pollutant burden (standard CPCB benchmarks: PM10=100 ug/m3, NO2=80 ug/m3, PM2.5 lag=60 ug/m3)
        double pm10Ratio = f.getOrDefault("pm10", 0.0) / 100.0;
        double no2Ratio = f.getOrDefault("no2", 0.0) / 80.0;
        double lagRatio = f.getOrDefault("pm25_spatial_lag_mean", 0.0) / 60.0;
        double sPollutant = Math.min(1.0, 0.40 * pm10Ratio + 0.30 * no2Ratio + 0.30 * lagRatio);

        // 2. Atmospheric Stagnation factor (low wind speed traps pollutants, high wind disperses)
        // wind_u and wind_v are in m/s; wind_speed in km/h or m/s
        double wsKmh = f.getOrDefault("wind_speed", 5.0);
        double wsMps = wsKmh / 3.6;
        double sStagnation;
        if (wsMps < 1.0) {
            sStagnation = 1.0; // Stagnant air
        } else if (wsMps < 2.5) {
            sStagnation = 0.75;
        } else if (wsMps < 5.0) {
            sStagnation = 0.45;
        } else {
            sStagnation = 0.20; // Well-ventilated
        }

        // 3. Fire and Industrial impact
        double fireDecay = f.getOrDefault("fire_frp_distance_decay", 0.0);
        double sFire = Math.min(1.0, fireDecay / 25.0);

        double isIndustrial = f.getOrDefault("industrial_zone_within_2km_flag", 0.0);
        double sIndustrial = isIndustrial > 0.5 ? 1.0 : 0.2;

        // 4. Combined deterministic score
        double rawRisk = 0.60 * sPollutant + 0.25 * sStagnation + 0.10 * sFire + 0.05 * sIndustrial;
        double riskScore = Math.min(1.0, Math.max(0.0, rawRisk));

        // 5. Confidence estimation based on spatial topology
        double baseConfidence = 0.85;
        double gapFlag = f.getOrDefault("monitoring_coverage_gap_flag", 0.0);
        if (gapFlag > 0.5) {
            baseConfidence -= 0.15; // Penalty for distance > 7.0 km from monitor
        }
        double confidence = Math.max(0.30, Math.min(1.0, baseConfidence));

        HotspotRiskLevel riskLevel = HotspotRiskLevel.fromScore(riskScore);

        metadata.put("evaluationMode", "FULL_PHYSICAL_BASELINE");
        metadata.put("sPollutant", roundTo4(sPollutant));
        metadata.put("sStagnation", roundTo4(sStagnation));
        metadata.put("sFire", roundTo4(sFire));

        return new HotspotPredictionResult(
                roundTo4(riskScore),
                riskLevel,
                roundTo4(confidence),
                ENGINE_VERSION,
                ENGINE_TYPE,
                metadata
        );
    }

    private static double roundTo4(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
