package com.aerosentinel.dto.federated;

import java.time.Instant;
import java.util.UUID;

public record FederatedNodeResponse(
    String nodeId,
    String nodeName,
    UUID cityId,
    String status,
    String modelVersion,
    Instant lastSeenAt,
    String endpointUrl
) {}
