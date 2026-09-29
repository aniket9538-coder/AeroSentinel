package com.aerosentinel.citizen;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller handling citizen environmental report submissions,
 * status inquiries, and photo access.
 */
@RestController
@RequestMapping("/api/v1/citizen")
public class CitizenReportController {

    private final CitizenReportService citizenReportService;
    private final PhotoStorageService photoStorageService;

    public CitizenReportController(
            CitizenReportService citizenReportService,
            PhotoStorageService photoStorageService
    ) {
        this.citizenReportService = citizenReportService;
        this.photoStorageService = photoStorageService;
    }

    /**
     * Multipart form ingestion endpoint supporting real citizen photos, GPS coordinates,
     * category, and qualitative descriptions.
     */
    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CitizenReportResponseDto> submitMultipartReport(
            @RequestParam("cityId") UUID cityId,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam("category") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "observedAt", required = false) String observedAt
    ) {
        Instant parsedObservedAt = null;
        if (observedAt != null && !observedAt.isBlank()) {
            try {
                parsedObservedAt = Instant.parse(observedAt.trim());
            } catch (Exception ignored) {
                parsedObservedAt = Instant.now();
            }
        }

        CitizenReportResponseDto response = citizenReportService.submitReport(
                cityId,
                latitude,
                longitude,
                category,
                description,
                photo,
                parsedObservedAt
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Preserved JSON ingestion endpoint for backward compatibility with programmatic callers.
     */
    @PostMapping(value = "/reports", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CitizenReport> submitJsonReport(@RequestBody CitizenReport report) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citizenReportService.createReport(report));
    }

    /**
     * Retrieves an individual citizen report and its Gemini Vision interpretation by ID.
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<CitizenReportResponseDto> getReportById(@PathVariable UUID reportId) {
        return ResponseEntity.ok(citizenReportService.getReportById(reportId));
    }

    /**
     * Lists all citizen reports for a specific city.
     */
    @GetMapping("/reports")
    public ResponseEntity<List<CitizenReportResponseDto>> getReports(@RequestParam UUID cityId) {
        return ResponseEntity.ok(citizenReportService.getReportsByCity(cityId));
    }

    /**
     * Securely serves stored citizen observation photos.
     */
    @GetMapping("/photos/{storageKey:[a-zA-Z0-9._-]+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String storageKey) {
        Resource resource = photoStorageService.loadPhotoAsResource(storageKey);
        Path path = photoStorageService.resolvePhotoPath(storageKey);
        String contentType = "image/jpeg";
        try {
            String probe = Files.probeContentType(path);
            if (probe != null) {
                contentType = probe;
            }
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
