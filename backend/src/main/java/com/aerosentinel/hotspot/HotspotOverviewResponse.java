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
        List<HotspotCellDto> cells
) {}
