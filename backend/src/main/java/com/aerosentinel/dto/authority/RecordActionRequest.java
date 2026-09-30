package com.aerosentinel.dto.authority;

import jakarta.validation.constraints.NotBlank;

public record RecordActionRequest(
        @NotBlank(message = "actionType is required (FIELD_VERIFICATION, MOBILE_SENSOR_DEPLOYED, SITE_CHECK, EMISSION_HALTED, WARNING_ISSUED)")
        String actionType,

        String notes,

        String result
) {}