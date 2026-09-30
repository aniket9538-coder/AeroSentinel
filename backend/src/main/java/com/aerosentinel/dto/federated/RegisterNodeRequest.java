package com.aerosentinel.dto.federated;

import java.util.UUID;

public record RegisterNodeRequest(
    String nodeId,
    UUID cityId,
    String nodeName,
    String endpointUrl
) {}
