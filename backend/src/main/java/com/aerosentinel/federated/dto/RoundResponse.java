package com.aerosentinel.federated.dto;

import java.time.Instant;
import java.util.List;

public record RoundResponse(
        String roundId,
        String baseModelVersion,
        String status,
        int minQuorum,
        List<String> participatingNodes,
        Instant createdAt
) {}
