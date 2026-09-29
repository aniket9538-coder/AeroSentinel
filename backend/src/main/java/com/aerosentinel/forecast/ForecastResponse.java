package com.aerosentinel.forecast;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Authoritative response contract for F4 multi-horizon PM2.5 forecasts.
 * Matches P3/P4 locked API contract with strictly null forecast confidence.
 *
 * Explicitly separates:
 *   - baseTimestamp (T0): base observation timestamp of parent prediction/feature snapshot
 *   - generatedAt: wall-clock timestamp when forecast ML inference was executed
 *   - targetTime: forecast target timestamp (T0 + horizonHours)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ForecastResponse(
        String h3Index,
        UUID cityId,
        Instant baseTimestamp,
        Instant generatedAt,
        String modelVersion,
        UUID parentPredictionId,
        UUID featureSnapshotId,
        String status,
        String freshness,
        List<ForecastItem> forecasts,
        Double forecastConfidence
) {
    /**
     * Backward-compatible constructor deriving baseTimestamp from targetTime - horizonHours.
     */
    public ForecastResponse(
            String h3Index,
            UUID cityId,
            Instant generatedAt,
            String modelVersion,
            UUID parentPredictionId,
            UUID featureSnapshotId,
            String status,
            String freshness,
            List<ForecastItem> forecasts,
            Double forecastConfidence
    ) {
        this(
                h3Index,
                cityId,
                forecasts != null && !forecasts.isEmpty() && forecasts.get(0).targetTime() != null && forecasts.get(0).horizonHours() != null
                        ? forecasts.get(0).targetTime().minus(Duration.ofHours(forecasts.get(0).horizonHours()))
                        : null,
                generatedAt,
                modelVersion,
                parentPredictionId,
                featureSnapshotId,
                status,
                freshness,
                forecasts,
                forecastConfidence
        );
    }

    public record ForecastItem(
            Integer horizonHours,
            Instant targetTime,
            Double predictedPm25,
            Double lowerBound,
            Double upperBound,
            String unit
    ) {}
}
