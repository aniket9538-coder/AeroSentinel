package com.aerosentinel.dto.citizen;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

public record CreateCitizenReportRequest(
        @NotNull(message = "cityId is required")
        UUID cityId,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "latitude must be <= 90.0")
        Double latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "longitude must be <= 180.0")
        Double longitude,

        @NotBlank(message = "category is required")
        String category,

        @Size(max = 2000, message = "description cannot exceed 2000 characters")
        String description,

        Instant observedAt,

        @NotNull(message = "photo file is required")
        MultipartFile image
) {}