package com.aerosentinel.federated.dto;

import java.util.List;

public record StartRoundRequest(
        String roundId,
        String baseVersion,
        Integer minQuorum,
        List<String> participatingNodes
) {}
