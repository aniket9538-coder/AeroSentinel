package com.aerosentinel.hotspot;

import java.time.Instant;
import java.util.UUID;

/**
 * Authoritative Spatial Context contract for a Hotspot H3 Cell.
 *
 * Serves as the single spatial and contextual anchor connecting:
 * - F2 Physical Telemetry (Air & Weather Observations)
 * - F3 Hotspot Detection & Risk Classification
 * - Future F4 PM2.5 Forecast (attaches via predictionId + h3Index + cityId + observedAt)
 * - Future F5 Evidence & Gemini Explanation (attaches via predictionId + h3Index + spatialContext)
 *
 * Guarantees:
 * 1. Single spatial anchor: H3 Cell Index (Resolution 8).
 * 2. Stable downstream identifiers (predictionId, h3Index, cityId, featureSnapshotId).
 * 3. Explicit data quality for all contextual sensors: VALID, MISSING, or UNAVAILABLE (no fake values).
 * 4. Transparent multi-city attribution (ML vs BASELINE engine metadata).
 */
public record HotspotSpatialContext(
        // Core Identification & Provenance
        UUID predictionId,
        String h3Index,
        UUID cityId,
        String cityName,
        UUID featureSnapshotId,
        Instant predictedAt,

        // Risk & Model Attribution
        double riskScore,
        String riskLevel,
        double confidence,
        String engineType,
        String modelVersion,
        String freshness,

        // Sub-Contexts
        AirContext airContext,
        WeatherContext weatherContext,
        MonitoringCoverageContext monitoringCoverage,
        SpatialDispersionContext spatialDispersion,
        EnvironmentalGisContext environmentalGis,

        // Hotspot Determination & Threshold Contract (Mandatory F3->F4)
        boolean isHotspot,
        Double operationalThreshold,
        ConfidenceBreakdown confidenceBreakdown
) {

    // Backwards-compatible 17-parameter constructor
    public HotspotSpatialContext(
            UUID predictionId,
            String h3Index,
            UUID cityId,
            String cityName,
            UUID featureSnapshotId,
            Instant predictedAt,
            double riskScore,
            String riskLevel,
            double confidence,
            String engineType,
            String modelVersion,
            String freshness,
            AirContext airContext,
            WeatherContext weatherContext,
            MonitoringCoverageContext monitoringCoverage,
            SpatialDispersionContext spatialDispersion,
            EnvironmentalGisContext environmentalGis
    ) {
        this(predictionId, h3Index, cityId, cityName, featureSnapshotId, predictedAt,
                riskScore, riskLevel, confidence, engineType, modelVersion, freshness,
                airContext, weatherContext, monitoringCoverage, spatialDispersion, environmentalGis,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? riskScore >= 0.20 : riskScore >= 0.40,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? 0.20 : 0.40,
                ConfidenceBreakdown.defaultForConfidence(confidence,
                        monitoringCoverage != null ? monitoringCoverage.nearestStationDistanceKm() : null,
                        monitoringCoverage != null ? monitoringCoverage.monitoringCoverageGapFlag() : null)
        );
    }

    public record ConfidenceBreakdown(
            Double overallConfidence,
            Double dataQualityScore,
            Double spatialCoverageConfidence,
            Double modelCertainty,
            Double nearestStationDistanceKm,
            Integer epistemicUncertaintyFlag
    ) {
        public static ConfidenceBreakdown defaultForConfidence(double confidence, Double distanceKm, Integer gapFlag) {
            double covConf = (gapFlag != null && gapFlag == 1) || (distanceKm != null && distanceKm > 7.0)
                    ? Math.max(0.15, Math.round(Math.exp(-Math.max(0.0, (distanceKm != null ? distanceKm : 10.0) - 5.0) / 25.0) * 1000.0) / 1000.0)
                    : 0.95;
            return new ConfidenceBreakdown(
                    confidence,
                    0.90,
                    covConf,
                    0.85,
                    distanceKm != null ? distanceKm : 0.0,
                    (gapFlag != null && gapFlag == 1) ? 1 : 0
            );
        }
    }

    public record AirContext(
            String dataQuality, // "VALID", "MISSING", "UNAVAILABLE"
            Double pm25,
            Double pm10,
            Double no2,
            Double so2,
            Double co,
            Double o3,
            Instant observedAt,
            String stationId,
            Double recentPm25Mean24h
    ) {
        public static AirContext unavailable() {
            return new AirContext("UNAVAILABLE", null, null, null, null, null, null, null, null, null);
        }

        public static AirContext missing() {
            return new AirContext("MISSING", null, null, null, null, null, null, null, null, null);
        }
    }

    public record WeatherContext(
            String dataQuality, // "VALID", "MISSING", "UNAVAILABLE"
            Double temperature,
            Double humidity,
            Double windSpeedKmh,
            Double windSpeedMps,
            Double windDirection,
            Double surfacePressure,
            Double precipitation,
            Instant observedAt
    ) {
        public static WeatherContext unavailable() {
            return new WeatherContext("UNAVAILABLE", null, null, null, null, null, null, null, null);
        }

        public static WeatherContext missing() {
            return new WeatherContext("MISSING", null, null, null, null, null, null, null, null);
        }
    }

    public record MonitoringCoverageContext(
            String dataQuality, // "VALID", "MISSING", "UNAVAILABLE"
            Double nearestStationDistanceKm,
            Integer stationsWithin5kmCount,
            Integer monitoringCoverageGapFlag,
            Double spatialCoverageConfidence
    ) {
        public static MonitoringCoverageContext unavailable() {
            return new MonitoringCoverageContext("UNAVAILABLE", null, null, null, null);
        }

        // Backwards-compatible 4-parameter constructor
        public MonitoringCoverageContext(
                String dataQuality,
                Double nearestStationDistanceKm,
                Integer stationsWithin5kmCount,
                Integer monitoringCoverageGapFlag
        ) {
            this(dataQuality, nearestStationDistanceKm, stationsWithin5kmCount, monitoringCoverageGapFlag,
                    (monitoringCoverageGapFlag != null && monitoringCoverageGapFlag == 1) || (nearestStationDistanceKm != null && nearestStationDistanceKm > 7.0)
                            ? Math.max(0.15, Math.round(Math.exp(-Math.max(0.0, (nearestStationDistanceKm != null ? nearestStationDistanceKm : 10.0) - 5.0) / 25.0) * 1000.0) / 1000.0)
                            : 0.95);
        }
    }

    public record SpatialDispersionContext(
            String dataQuality, // "VALID", "MISSING", "UNAVAILABLE"
            Double pm25SpatialLagMean,
            Double windU,
            Double windV
    ) {
        public static SpatialDispersionContext unavailable() {
            return new SpatialDispersionContext("UNAVAILABLE", null, null, null);
        }
    }

    public record EnvironmentalGisContext(
            String dataQuality, // "VALID", "MISSING", "UNAVAILABLE"
            Double distToNearestIndustrialKm,
            Double distToNearestMajorRoadKm,
            Integer sensitiveReceptorsCount2km,
            Integer industrialZoneWithin2kmFlag,
            Integer fireCount24h25km,
            Double fireFrpSum24h25km,
            Double fireFrpMean24h25km,
            Double nearestFireDistanceKm,
            Double fireFrpDistanceDecay,
            Double fireUpwindAlignmentScore
    ) {
        public static EnvironmentalGisContext unavailable() {
            return new EnvironmentalGisContext("UNAVAILABLE", null, null, null, null, null, null, null, null, null, null);
        }
    }
}
