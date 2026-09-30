package com.aerosentinel.dto.authority;

import jakarta.validation.constraints.NotBlank;

public record CompleteInspectionRequest(
        @NotBlank(message = "fieldNotes are required")
        String fieldNotes,

        @NotBlank(message = "verificationStatus is required (VERIFIED, NOT_VERIFIED, INCONCLUSIVE)")
        String verificationStatus
) {}