package com.aerosentinel.dto.citizen;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CitizenReportResponse(
        String reportId,
        UUID cityId,
        UUID userId,
        Double latitude,
        Double longitude,
        String h3CellId,
        String category,
        String description,
        String photoUrl,
        String status,
        String verificationStatus,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        GeminiVisionResponse geminiAnalysis
) {}
