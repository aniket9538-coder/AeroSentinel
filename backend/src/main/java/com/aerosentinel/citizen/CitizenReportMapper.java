package com.aerosentinel.citizen;

import com.aerosentinel.dto.citizen.CitizenReportResponse;
import com.aerosentinel.dto.citizen.GeminiVisionResponse;
import com.aerosentinel.dto.citizen.ReportStatusResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class CitizenReportMapper {

    private final ObjectMapper objectMapper;

    public CitizenReportMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CitizenReportResponse toResponse(CitizenReport report, GeminiAnalysis analysis) {
        GeminiVisionResponse visionResponse = null;
        if (analysis != null) {
            visionResponse = toVisionResponse(analysis);
        }

        return new CitizenReportResponse(
                report.getReportId(),
                report.getCityId(),
                report.getUserId(),
                report.getLatitude(),
                report.getLongitude(),
                report.getH3Index(),
                report.getCategory() != null ? report.getCategory() : null,
                report.getDescription(),
                report.getImageUrl(),
                report.getStatus() != null ? report.getStatus() : null,
                report.getVerificationStatus(),
                report.getSubmittedAt(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                visionResponse
        );
    }

    public ReportStatusResponse toStatusResponse(CitizenReport report) {
        return new ReportStatusResponse(
                report.getReportId(),
                report.getH3Index(),
                report.getStatus() != null ? report.getStatus() : null
        );
    }

    public GeminiVisionResponse toVisionResponse(GeminiAnalysis analysis) {
        List<String> visualIndicators = Collections.emptyList();
        String visibleCondition = analysis.getDetectedCategory();
        String limitations = null;

        if (analysis.getRawResponse() != null && !analysis.getRawResponse().isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(
                        analysis.getRawResponse(),
                        new TypeReference<>() {}
                );
                if (map.get("visualIndicators") instanceof List<?> list) {
                    visualIndicators = list.stream().map(Object::toString).toList();
                }
                if (map.get("visibleCondition") instanceof String vc) {
                    visibleCondition = vc;
                }
                if (map.get("limitations") instanceof String lim) {
                    limitations = lim;
                }
            } catch (Exception ignored) {
                // Keep default mapped properties if JSON parsing fails
            }
        }

        return new GeminiVisionResponse(
                analysis.getModelName(),
                analysis.getDetectedCategory(),
                visibleCondition,
                analysis.getConfidence(),
                visualIndicators,
                analysis.getNarrativeSummary(),
                analysis.getVerificationRequired(),
                limitations,
                analysis.getAnalyzedAt(),
                analysis.getAnalysisStatus() != null ? analysis.getAnalysisStatus().name() : null
        );
    }
}