package com.aerosentinel.federated.dto;

import java.time.Instant;
import java.util.UUID;

public record UpdateResponse(
        UUID updateId,
        String roundId,
        String cityName,
        String status,
        int sampleCount,
        Instant submittedAt
) {}
