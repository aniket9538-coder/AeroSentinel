package com.aerosentinel.citizen;

import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import com.aerosentinel.util.H3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CitizenReportUnitTest {

    private CitizenReportRepository citizenReportRepository;
    private GeminiAnalysisRepository geminiAnalysisRepository;
    private PhotoStorageService photoStorageService;
    private CitizenVisionAiClient visionAiClient;
    private CitizenReportService citizenReportService;

    private static final UUID PUNE_CITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final double PUNE_LAT = 18.5304;
    private static final double PUNE_LNG = 73.8467;

    @BeforeEach
    void setUp() {
        citizenReportRepository = mock(CitizenReportRepository.class);
        geminiAnalysisRepository = mock(GeminiAnalysisRepository.class);
        photoStorageService = mock(PhotoStorageService.class);
        visionAiClient = mock(CitizenVisionAiClient.class);

        citizenReportService = new CitizenReportService(
                citizenReportRepository,
                geminiAnalysisRepository,
                photoStorageService,
                visionAiClient
        );
    }

    @Test
    @DisplayName("Successfully submits multipart report with photo, validates coordinates, derives H3 res 8, and persists GeminiAnalysis")
    void testSuccessfulMultipartReportSubmission() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "smoke_plume.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        Path dummyPath = Paths.get("dummy/path/smoke_plume.jpg");
        when(photoStorageService.storePhoto(any())).thenReturn(
                new PhotoStorageService.StoredPhoto("test-key.jpg", "/api/v1/citizen/photos/test-key.jpg", dummyPath, 4, "image/jpeg")
        );

        UUID generatedReportId = UUID.randomUUID();
        when(citizenReportRepository.save(any(CitizenReport.class))).thenAnswer(inv -> {
            CitizenReport r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(generatedReportId);
            }
            return r;
        });

        CitizenVisionAiClient.CitizenVisionResultDto visionDto = new CitizenVisionAiClient.CitizenVisionResultDto(
                "SUCCESS",
                generatedReportId.toString(),
                "8860144aa1fffff",
                "SMOKE_LIKE",
                0.88,
                List.of("Dense smoke plume", "Atmospheric haze"),
                List.of("Cannot determine numerical PM2.5", "Cannot determine legal fault"),
                Map.of("smoke_visible", true, "fire_visible", false),
                List.of("PII scan cleared"),
                "gemini-2.0-flash",
                "vision_analysis_v001"
        );
        when(visionAiClient.analyzeImage(any(), any(), any())).thenReturn(visionDto);

        when(geminiAnalysisRepository.save(any(GeminiAnalysis.class))).thenAnswer(inv -> {
            GeminiAnalysis ga = inv.getArgument(0);
            ga.setId(UUID.randomUUID());
            return ga;
        });

        CitizenReportResponseDto response = citizenReportService.submitReport(
                PUNE_CITY_ID,
                PUNE_LAT,
                PUNE_LNG,
                "smoke",
                "Dense black smoke emerging from garbage dump near river bridge",
                photo,
                Instant.now()
        );

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(generatedReportId);
        assertThat(response.cityId()).isEqualTo(PUNE_CITY_ID);
        assertThat(response.latitude()).isEqualTo(PUNE_LAT);
        assertThat(response.longitude()).isEqualTo(PUNE_LNG);
        assertThat(response.category()).isEqualTo("SMOKE");
        assertThat(response.status()).isEqualTo("ANALYZED");

        // Verify H3 resolution 8 derivation
        String expectedH3 = H3Utils.coordinatesToH3(PUNE_LAT, PUNE_LNG, 8);
        assertThat(response.h3Index()).isEqualTo(expectedH3);

        // Verify GeminiAnalysis persistence
        ArgumentCaptor<GeminiAnalysis> analysisCaptor = ArgumentCaptor.forClass(GeminiAnalysis.class);
        verify(geminiAnalysisRepository, times(1)).save(analysisCaptor.capture());
        GeminiAnalysis capturedAnalysis = analysisCaptor.getValue();
        assertThat(capturedAnalysis.getCitizenReportId()).isEqualTo(generatedReportId);
        assertThat(capturedAnalysis.getDetectedCategory()).isEqualTo("SMOKE_LIKE");
        assertThat(capturedAnalysis.getConfidence()).isEqualTo(0.88);
        assertThat(capturedAnalysis.getH3Index()).isEqualTo(expectedH3);
        assertThat(capturedAnalysis.getIsGrounded()).isTrue();

        // Verify response contains vision analysis
        assertThat(response.visionAnalysis()).isNotNull();
        assertThat(response.visionAnalysis().detectedCategory()).isEqualTo("SMOKE_LIKE");
        assertThat(response.visionAnalysis().confidence()).isEqualTo(0.88);
    }

    @Test
    @DisplayName("Report survives and is preserved even when Gemini Vision fails or times out")
    void testReportSurvivesGeminiFailure() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "fire.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        when(photoStorageService.storePhoto(any())).thenReturn(
                new PhotoStorageService.StoredPhoto("test-key.jpg", "/api/v1/citizen/photos/test-key.jpg", Paths.get("dummy.jpg"), 4, "image/jpeg")
        );

        UUID generatedReportId = UUID.randomUUID();
        when(citizenReportRepository.save(any(CitizenReport.class))).thenAnswer(inv -> {
            CitizenReport r = inv.getArgument(0);
            if (r.getId() == null) r.setId(generatedReportId);
            return r;
        });

        // Simulate vision service throwing an exception
        when(visionAiClient.analyzeImage(any(), any(), any())).thenThrow(new RuntimeException("Vision API connection timeout"));

        CitizenReportResponseDto response = citizenReportService.submitReport(
                PUNE_CITY_ID,
                PUNE_LAT,
                PUNE_LNG,
                "BURNING",
                "Open waste fire spotted",
                photo,
                Instant.now()
        );

        // Report must still exist!
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(generatedReportId);
        verify(citizenReportRepository, atLeastOnce()).save(any(CitizenReport.class));

        // Vision summary indicates UNAVAILABLE
        assertThat(response.visionAnalysis()).isNotNull();
        assertThat(response.visionAnalysis().analysisStatus()).isEqualTo("UNAVAILABLE");
        assertThat(response.visionAnalysis().observations().get(0)).contains("citizen report is still stored");
    }

    @Test
    @DisplayName("Rejects report when latitude or longitude is missing")
    void testRejectsMissingCoordinates() {
        assertThatThrownBy(() -> citizenReportService.submitReport(
                PUNE_CITY_ID,
                null,
                PUNE_LNG,
                "SMOKE",
                "Plume observed",
                null,
                Instant.now()
        )).isInstanceOf(ValidationException.class)
          .hasMessageContaining("coordinates (latitude and longitude) are mandatory");
    }

    @Test
    @DisplayName("Rejects report with invalid latitude or longitude out of geographic bounds")
    void testRejectsOutOfBoundsCoordinates() {
        assertThatThrownBy(() -> citizenReportService.submitReport(
                PUNE_CITY_ID,
                95.0, // Invalid latitude
                PUNE_LNG,
                "SMOKE",
                "Plume observed",
                null,
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejects report with description exceeding 1000 characters")
    void testRejectsExcessiveDescription() {
        String giantDescription = "A".repeat(1001);
        assertThatThrownBy(() -> citizenReportService.submitReport(
                PUNE_CITY_ID,
                PUNE_LAT,
                PUNE_LNG,
                "SMOKE",
                giantDescription,
                null,
                Instant.now()
        )).isInstanceOf(ValidationException.class)
          .hasMessageContaining("exceeds maximum permitted length of 1000 characters");
    }

    @Test
    @DisplayName("Normalizes category aliases (e.g. 'odour' -> 'ODOR', case insensitivity)")
    void testNormalizesCategory() {
        UUID generatedReportId = UUID.randomUUID();
        when(citizenReportRepository.save(any(CitizenReport.class))).thenAnswer(inv -> {
            CitizenReport r = inv.getArgument(0);
            if (r.getId() == null) r.setId(generatedReportId);
            return r;
        });

        CitizenReportResponseDto res1 = citizenReportService.submitReport(
                PUNE_CITY_ID, PUNE_LAT, PUNE_LNG, "odour", "Pungent chemical smell", null, null
        );
        assertThat(res1.category()).isEqualTo("ODOR");

        CitizenReportResponseDto res2 = citizenReportService.submitReport(
                PUNE_CITY_ID, PUNE_LAT, PUNE_LNG, "dust", "Road dust", null, null
        );
        assertThat(res2.category()).isEqualTo("DUST");
    }

    @Test
    @DisplayName("Retrieves report by ID along with its attached Gemini Vision interpretation")
    void testGetReportById() {
        UUID reportId = UUID.randomUUID();
        CitizenReport report = new CitizenReport();
        report.setId(reportId);
        report.setCityId(PUNE_CITY_ID);
        report.setLatitude(PUNE_LAT);
        report.setLongitude(PUNE_LNG);
        report.setH3Index("8860144aa1fffff");
        report.setCategory("SMOKE");
        report.setDescription("Smoke plume");
        report.setStatus("ANALYZED");

        when(citizenReportRepository.findById(reportId)).thenReturn(Optional.of(report));

        GeminiAnalysis ga = new GeminiAnalysis();
        ga.setId(UUID.randomUUID());
        ga.setCitizenReportId(reportId);
        ga.setDetectedCategory("SMOKE_LIKE");
        ga.setConfidence(0.92);
        ga.setNarrativeSummary("Visible dark particulate plume proximate to surface");
        ga.setModelVersion("gemini-2.0-flash");

        when(geminiAnalysisRepository.findTopByCitizenReportIdOrderByCreatedAtDesc(reportId))
                .thenReturn(Optional.of(ga));

        CitizenReportResponseDto result = citizenReportService.getReportById(reportId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(reportId);
        assertThat(result.visionAnalysis()).isNotNull();
        assertThat(result.visionAnalysis().detectedCategory()).isEqualTo("SMOKE_LIKE");
        assertThat(result.visionAnalysis().confidence()).isEqualTo(0.92);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when report ID does not exist")
    void testGetReportByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(citizenReportRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> citizenReportService.getReportById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Citizen report not found with id");
    }
}
