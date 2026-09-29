package com.aerosentinel.dto.inspection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record SubmitVerificationRequest(
        @NotNull String verificationResult,
        @NotBlank String observedConditions,
        String inspectorNotes,
        String evidenceReferences,
        String verifiedBy,
        Instant inspectedAt
) {}
