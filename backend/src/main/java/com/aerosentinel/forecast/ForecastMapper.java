package com.aerosentinel.forecast;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Mapper for converting Forecast JPA entities to public ForecastResponse DTOs.
 */
public final class ForecastMapper {

    private ForecastMapper() {}

    public static ForecastResponse toResponse(List<Forecast> forecasts) {
        if (forecasts == null || forecasts.isEmpty()) {
            return null;
        }

        List<Forecast> sorted = forecasts.stream()
                .sorted(Comparator.comparingInt(Forecast::getHorizonHours))
                .toList();

        Forecast first = sorted.get(0);
        Instant now = Instant.now();
        String freshness = computeFreshness(first.getGeneratedAt(), now);

        List<ForecastResponse.ForecastItem> items = sorted.stream()
                .map(f -> new ForecastResponse.ForecastItem(
                        f.getHorizonHours(),
                        f.getTargetTime(),
                        f.getPredictedPm25(),
                        f.getLowerBound(),
                        f.getUpperBound(),
                        f.getUnit()
                ))
                .toList();

        Instant baseTimestamp = first.getTargetTime().minus(Duration.ofHours(first.getHorizonHours()));

        return new ForecastResponse(
                first.getH3Index(),
                first.getCityId(),
                baseTimestamp,
                first.getGeneratedAt(),
                first.getModelVersion(),
                first.getParentPredictionId(),
                first.getFeatureSnapshotId(),
                first.getStatus(),
                freshness,
                items,
                null // forecastConfidence is strictly null
        );
    }

    public static String computeFreshness(Instant generatedAt, Instant now) {
        if (generatedAt == null) {
            return "NO_DATA";
        }
        Duration age = Duration.between(generatedAt, now);
        if (age.isNegative() || age.toHours() <= 2) {
            return "LIVE";
        } else if (age.toHours() <= 24) {
            return "STALE";
        } else {
            return "UNAVAILABLE";
        }
    }
}
