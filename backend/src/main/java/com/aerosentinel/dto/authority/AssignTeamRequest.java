package com.aerosentinel.dto.authority;

import jakarta.validation.constraints.NotBlank;

public record AssignTeamRequest(
        @NotBlank(message = "teamId is required (e.g. FIELD-TEAM-01)")
        String teamId,

        String notes
) {}