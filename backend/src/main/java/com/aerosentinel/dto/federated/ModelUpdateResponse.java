package com.aerosentinel.dto.federated;

import java.time.Instant;

public record ModelUpdateResponse(
    String roundId,
    String nodeId,
    String status,
    int sampleCount,
    Instant submittedAt
) {}
