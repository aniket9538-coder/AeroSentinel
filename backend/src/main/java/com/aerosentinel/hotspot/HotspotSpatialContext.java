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
        EnvironmentalGisContext environmentalGis
) {

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
            Integer monitoringCoverageGapFlag
    ) {
        public static MonitoringCoverageContext unavailable() {
            return new MonitoringCoverageContext("UNAVAILABLE", null, null, null);
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
