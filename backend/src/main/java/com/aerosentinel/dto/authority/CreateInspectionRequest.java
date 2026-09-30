package com.aerosentinel.dto.authority;

import jakarta.validation.constraints.NotBlank;

public record CreateInspectionRequest(
        @NotBlank(message = "teamId is required")
        String teamId
) {}