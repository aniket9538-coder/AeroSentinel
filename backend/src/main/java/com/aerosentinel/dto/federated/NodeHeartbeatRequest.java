package com.aerosentinel.dto.federated;

public record NodeHeartbeatRequest(
    String status,
    String modelVersion
) {}
