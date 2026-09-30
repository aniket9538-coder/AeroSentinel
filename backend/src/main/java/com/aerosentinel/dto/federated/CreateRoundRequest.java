package com.aerosentinel.dto.federated;

import java.util.List;

public record CreateRoundRequest(
    String roundId,
    String baseModelVersion,
    List<String> participatingNodes,
    Integer minQuorum
) {}
