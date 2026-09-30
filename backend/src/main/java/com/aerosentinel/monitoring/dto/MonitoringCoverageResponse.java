package com.aerosentinel.monitoring.dto;

import java.util.UUID;

/**
 * Authoritative response contract for F8 monitoring coverage.
 * Represents nearest active station proximity, local station density,
 * and coverage gap classification for an H3 cell.
 */
public record MonitoringCoverageResponse(
        String h3Index,
        Double latitude,
        Double longitude,
        UUID nearestStationId,
        String nearestStationCode,
        String nearestStationName,
        Double nearestStationLatitude,
        Double nearestStationLongitude,
        Double nearestStationDistanceKm,
        Integer stationsWithin5kmCount,
        Integer monitoringCoverageGapFlag
) {
    /**
     * Factory method for creating a safe "no coverage available" response
     * when a city or region has no active monitoring stations.
     * Does NOT fabricate stations or zero distances.
     */
    public static MonitoringCoverageResponse noCoverage(String h3Index, Double latitude, Double longitude) {
        return new MonitoringCoverageResponse(
                h3Index,
                latitude,
                longitude,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                1
        );
    }
}
