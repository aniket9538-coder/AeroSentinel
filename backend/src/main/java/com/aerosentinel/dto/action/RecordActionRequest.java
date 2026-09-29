package com.aerosentinel.dto.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecordActionRequest(
        @NotNull(message = "Alert ID is required") UUID alertId,
        @NotBlank(message = "Action type is required") String actionType,
        @NotBlank(message = "Action notes/details are required") String notes,
        String performedBy
) {}
