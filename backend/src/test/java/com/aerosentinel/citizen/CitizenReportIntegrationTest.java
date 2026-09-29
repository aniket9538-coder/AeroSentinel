package com.aerosentinel.citizen;

import com.aerosentinel.alert.AlertRepository;
import com.aerosentinel.city.City;
import com.aerosentinel.city.CityRepository;
import com.aerosentinel.model.GeminiAnalysis;
import com.aerosentinel.repository.GeminiAnalysisRepository;
import com.aerosentinel.util.H3Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CitizenReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CitizenReportRepository citizenReportRepository;

    @Autowired
    private GeminiAnalysisRepository geminiAnalysisRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CityRepository cityRepository;

    private UUID puneCityId;

    // Real coordinates in Shivajinagar, Pune Metropolitan Region
    private static final double REAL_PUNE_LAT = 18.5304;
    private static final double REAL_PUNE_LNG = 73.8467;

    // Standard valid JPEG header bytes
    private static final byte[] SAMPLE_JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};

    @BeforeEach
    void setUp() {
        City city = cityRepository.findByNameIgnoreCase("Pune")
                .orElseGet(() -> {
                    City c = new City();
                    c.setName("Pune");
                    c.setState("Maharashtra");
                    c.setCountry("India");
                    c.setLatitude(REAL_PUNE_LAT);
                    c.setLongitude(REAL_PUNE_LNG);
                    return cityRepository.save(c);
                });
        puneCityId = city.getId();
    }

    @Test
    @DisplayName("End-to-end integration: multipart submission -> H3 res 8 -> citizen_reports -> GeminiAnalysis -> GET report -> No direct alert")
    void testEndToEndCitizenReportFlow() throws Exception {
        long alertCountBefore = alertRepository.count();

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "shivajinagar_smoke.jpg",
                "image/jpeg",
                SAMPLE_JPEG_BYTES
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .file(photo)
                        .param("cityId", puneCityId.toString())
                        .param("latitude", String.valueOf(REAL_PUNE_LAT))
                        .param("longitude", String.valueOf(REAL_PUNE_LNG))
                        .param("category", "SMOKE")
                        .param("description", "Heavy localized black smoke plume observed near transport terminal.")
                        .param("observedAt", "2026-09-28T12:00:00Z"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.h3Index").exists())
                .andExpect(jsonPath("$.category").value("SMOKE"))
                .andExpect(jsonPath("$.status").exists())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID reportId = UUID.fromString(responseJson.get("id").asText());
        String h3Index = responseJson.get("h3Index").asText();

        // 1. Verify H3 resolution 8 derivation
        String expectedH3 = H3Utils.coordinatesToH3(REAL_PUNE_LAT, REAL_PUNE_LNG, 8);
        assertThat(h3Index).isEqualTo(expectedH3);
        assertThat(h3Index).hasSize(15);

        // 2. Verify PostgreSQL persistence in citizen_reports
        CitizenReport persistedReport = citizenReportRepository.findById(reportId).orElse(null);
        assertThat(persistedReport).isNotNull();
        assertThat(persistedReport.getCityId()).isEqualTo(puneCityId);
        assertThat(persistedReport.getH3Index()).isEqualTo(expectedH3);
        assertThat(persistedReport.getImageUrl()).isNotNull();
        assertThat(persistedReport.getStatus()).isIn("ANALYZED", "PENDING");

        // 3. Verify PostgreSQL persistence in gemini_analyses with foreign key citizen_report_id
        List<GeminiAnalysis> analyses = geminiAnalysisRepository.findByCitizenReportIdOrderByCreatedAtDesc(reportId);
        assertThat(analyses).isNotEmpty();
        GeminiAnalysis ga = analyses.get(0);
        assertThat(ga.getCitizenReportId()).isEqualTo(reportId);
        assertThat(ga.getH3Index()).isEqualTo(expectedH3);
        assertThat(ga.getDetectedCategory()).isNotNull();
        assertThat(ga.getConfidence()).isNotNull();
        assertThat(ga.getConfidence()).isBetween(0.0, 1.0);
        assertThat(ga.getIsGrounded()).isTrue();

        // 4. Verify GET /api/v1/citizen/reports/{reportId}
        mockMvc.perform(get("/api/v1/citizen/reports/" + reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.h3Index").value(expectedH3))
                .andExpect(jsonPath("$.visionAnalysis").exists())
                .andExpect(jsonPath("$.visionAnalysis.detectedCategory").value(ga.getDetectedCategory()));

        // 5. Verify Photo Retrieval endpoint
        String imageUrl = persistedReport.getImageUrl();
        String storageKey = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        mockMvc.perform(get("/api/v1/citizen/photos/" + storageKey))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));

        // 6. Strict Governance: Verify citizen report NEVER directly created an alert
        long alertCountAfter = alertRepository.count();
        assertThat(alertCountAfter).isEqualTo(alertCountBefore);
    }

    @Test
    @DisplayName("F6-P7 Failure Case 1: Rejects report missing mandatory geographic coordinates")
    void testFailureCase1_missingLocationReturnsBadRequest() throws Exception {
        long reportCountBefore = citizenReportRepository.count();

        mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .param("cityId", puneCityId.toString())
                        .param("category", "SMOKE")
                        .param("description", "Missing coordinates"))
                .andExpect(status().isBadRequest());

        assertThat(citizenReportRepository.count()).isEqualTo(reportCountBefore);
    }

    @Test
    @DisplayName("F6-P7 Failure Case 2: Rejects report with out-of-bounds latitude/longitude")
    void testFailureCase2_outOfBoundsCoordinatesReturnsBadRequest() throws Exception {
        long reportCountBefore = citizenReportRepository.count();

        mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .param("cityId", puneCityId.toString())
                        .param("latitude", "95.5")
                        .param("longitude", String.valueOf(REAL_PUNE_LNG))
                        .param("category", "SMOKE")
                        .param("description", "Out of bounds latitude"))
                .andExpect(status().isBadRequest());

        assertThat(citizenReportRepository.count()).isEqualTo(reportCountBefore);
    }

    @Test
    @DisplayName("F6-P7 Failure Case 3: Rejects unsupported file MIME type (e.g. application/pdf)")
    void testFailureCase3_unsupportedFileFormatReturnsBadRequest() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "photo",
                "evidence_document.pdf",
                "application/pdf",
                "%PDF-1.5 test content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .file(pdfFile)
                        .param("cityId", puneCityId.toString())
                        .param("latitude", String.valueOf(REAL_PUNE_LAT))
                        .param("longitude", String.valueOf(REAL_PUNE_LNG))
                        .param("category", "SMOKE")
                        .param("description", "Attempting PDF upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F6-P7 Failure Case 4: Rejects file with spoofed .jpg extension and invalid magic bytes")
    void testFailureCase4_fakeExtensionWithInvalidMagicBytesReturnsBadRequest() throws Exception {
        MockMultipartFile fakeJpg = new MockMultipartFile(
                "photo",
                "malicious_script.jpg",
                "image/jpeg",
                "echo 'not a real jpeg binary image content'".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .file(fakeJpg)
                        .param("cityId", puneCityId.toString())
                        .param("latitude", String.valueOf(REAL_PUNE_LAT))
                        .param("longitude", String.valueOf(REAL_PUNE_LNG))
                        .param("category", "SMOKE")
                        .param("description", "Spoofed file upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F6-P7 Failure Case 5: Rejects upload exceeding 15MB size limit")
    void testFailureCase5_oversizedPhotoReturnsBadRequest() throws Exception {
        byte[] oversizedBytes = new byte[16];
        System.arraycopy(SAMPLE_JPEG_BYTES, 0, oversizedBytes, 0, SAMPLE_JPEG_BYTES.length);

        MockMultipartFile oversizedFile = new MockMultipartFile(
                "photo",
                "giant_raw_image.jpg",
                "image/jpeg",
                oversizedBytes
        ) {
            @Override
            public long getSize() {
                return (15 * 1024 * 1024) + 1024; // 15MB + 1KB
            }
        };

        mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .file(oversizedFile)
                        .param("cityId", puneCityId.toString())
                        .param("latitude", String.valueOf(REAL_PUNE_LAT))
                        .param("longitude", String.valueOf(REAL_PUNE_LNG))
                        .param("category", "SMOKE")
                        .param("description", "Oversized upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("F6-P7 Text-Only Success: Allows submission without photo without creating synthetic Gemini analysis")
    void testTextOnlyReportSubmitsCleanlyWithoutGeminiAnalysis() throws Exception {
        long analysisCountBefore = geminiAnalysisRepository.count();

        MvcResult result = mockMvc.perform(multipart("/api/v1/citizen/reports")
                        .param("cityId", puneCityId.toString())
                        .param("latitude", String.valueOf(REAL_PUNE_LAT))
                        .param("longitude", String.valueOf(REAL_PUNE_LNG))
                        .param("category", "ODOR")
                        .param("description", "Pungent chemical odor detected near industrial gate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.category").value("ODOR"))
                .andExpect(jsonPath("$.visionAnalysis").doesNotExist())
                .andReturn();

        // Verify zero GeminiAnalysis rows were fabricated
        assertThat(geminiAnalysisRepository.count()).isEqualTo(analysisCountBefore);
    }
}
