package com.aerosentinel.dto.authority;

import jakarta.validation.constraints.NotBlank;

public record ResolveEventRequest(
        @NotBlank(message = "resolutionNotes are required")
        String resolutionNotes
) {}