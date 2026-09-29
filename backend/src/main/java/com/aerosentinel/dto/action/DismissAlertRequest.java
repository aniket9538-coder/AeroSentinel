package com.aerosentinel.dto.action;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DismissAlertRequest(
        @NotBlank(message = "Dismissal reason is required") String dismissalReason,
        UUID dismissedBy
) {}
