package com.aerosentinel.dto.inspection;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldTeamDto(
        UUID id,
        String teamCode,
        String teamName,
        UUID cityId,
        String status,
        String contactNumber,
        String leaderName,
        Instant createdAt
) {}
