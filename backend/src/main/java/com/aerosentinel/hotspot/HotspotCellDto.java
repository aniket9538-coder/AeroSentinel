package com.aerosentinel.hotspot;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for a Hotspot H3 Cell.
 *
 * Provides stable downstream keys for F4 Forecast and F5 Evidence:
 * - predictionId: Unique ID of the persisted prediction (FK for F4/F5)
 * - h3Index: 15-char Uber H3 index (the single spatial anchor)
 * - cityId / cityName: Geographic jurisdiction
 * - featureSnapshotId: Provenance tracing back to underlying 36-feature snapshot
 * - engineType / modelVersion: Explicit model attribution (ML vs BASELINE)
 * - spatialContext: Rich multi-sensor context (Air, Weather, Coverage, Dispersion, GIS)
 */
public record HotspotCellDto(
        String h3Index,
        UUID gridCellId,
        double riskScore,
        String riskLevel,
        double confidence,
        Instant predictedAt,
        String freshness,
        String modelVersion,
        UUID predictionId,
        UUID cityId,
        String cityName,
        String engineType,
        UUID featureSnapshotId,
        HotspotSpatialContext spatialContext,
        boolean isHotspot,
        Double operationalThreshold
) {
    // Backwards-compatible 14-parameter constructor
    public HotspotCellDto(
            String h3Index,
            UUID gridCellId,
            double riskScore,
            String riskLevel,
            double confidence,
            Instant predictedAt,
            String freshness,
            String modelVersion,
            UUID predictionId,
            UUID cityId,
            String cityName,
            String engineType,
            UUID featureSnapshotId,
            HotspotSpatialContext spatialContext
    ) {
        this(h3Index, gridCellId, riskScore, riskLevel, confidence, predictedAt, freshness, modelVersion,
                predictionId, cityId, cityName, engineType, featureSnapshotId, spatialContext,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? riskScore >= 0.20 : riskScore >= 0.40,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? 0.20 : 0.40);
    }

    // Backwards-compatible 8-parameter constructor
    public HotspotCellDto(
            String h3Index,
            UUID gridCellId,
            double riskScore,
            String riskLevel,
            double confidence,
            Instant predictedAt,
            String freshness,
            String modelVersion
    ) {
        this(h3Index, gridCellId, riskScore, riskLevel, confidence, predictedAt, freshness, modelVersion,
                null, null, null, null, null, null,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? riskScore >= 0.20 : riskScore >= 0.40,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? 0.20 : 0.40);
    }
}
