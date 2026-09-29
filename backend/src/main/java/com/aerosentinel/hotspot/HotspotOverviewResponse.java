package com.aerosentinel.hotspot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HotspotOverviewResponse(
        UUID cityId,
        String cityName,
        Instant generatedAt,
        String modelVersion,
        String engineType,
        String freshness,
        int totalCells,
        int highRiskCells,
        List<HotspotCellDto> cells,
        Double operationalThreshold
) {
    // Backwards-compatible 9-param constructor
    public HotspotOverviewResponse(
            UUID cityId,
            String cityName,
            Instant generatedAt,
            String modelVersion,
            String engineType,
            String freshness,
            int totalCells,
            int highRiskCells,
            List<HotspotCellDto> cells
    ) {
        this(cityId, cityName, generatedAt, modelVersion, engineType, freshness, totalCells, highRiskCells, cells,
                "hotspot_classifier_v1".equalsIgnoreCase(modelVersion) ? 0.20 : 0.40);
    }
}
