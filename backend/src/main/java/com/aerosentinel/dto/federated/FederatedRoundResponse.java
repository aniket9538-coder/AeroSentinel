package com.aerosentinel.dto.federated;

import java.time.Instant;

public record FederatedRoundResponse(
    String roundId,
    String baseModelVersion,
    String status,
    int minQuorum,
    int expectedNodes,
    Instant startedAt
) {}
