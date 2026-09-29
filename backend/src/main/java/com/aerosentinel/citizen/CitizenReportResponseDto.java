package com.aerosentinel.citizen;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Clean, safe client-facing DTO representing a citizen report and its attached Gemini Vision analysis.
 * Never exposes raw internal database secrets or unrestricted filesystem paths.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CitizenReportResponseDto(
        UUID id,
        String reportId,
        UUID cityId,
        Double latitude,
        Double longitude,
        String h3Index,
        String category,
        String description,
        String imageUrl,
        Instant submittedAt,
        String status,
        String verificationStatus,
        Instant createdAt,
        VisionAnalysisSummaryDto visionAnalysis
) {
    public record VisionAnalysisSummaryDto(
            UUID analysisId,
            String analysisStatus,
            String detectedCategory,
            Double confidence,
            List<String> observations,
            List<String> uncertainty,
            String modelVersion,
            String promptVersion,
            Instant analyzedAt
    ) {}

    public static CitizenReportResponseDto fromEntity(CitizenReport report, VisionAnalysisSummaryDto vision) {
        String repId = report.getId() != null ? "CR-" + report.getId().toString().substring(0, 8).toUpperCase() : null;
        return new CitizenReportResponseDto(
                report.getId(),
                repId,
                report.getCityId(),
                report.getLatitude(),
                report.getLongitude(),
                report.getH3Index(),
                report.getCategory(),
                report.getDescription(),
                report.getImageUrl(),
                report.getSubmittedAt(),
                report.getStatus(),
                report.getVerificationStatus(),
                report.getCreatedAt(),
                vision
        );
    }
}
