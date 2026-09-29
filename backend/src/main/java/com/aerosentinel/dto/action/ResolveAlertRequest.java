package com.aerosentinel.dto.action;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ResolveAlertRequest(
        @NotBlank(message = "Resolution notes are required") String resolutionNotes,
        UUID resolvedBy
) {}
