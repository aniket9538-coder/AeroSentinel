package com.aerosentinel.dto.authority;

import java.time.Instant;
import java.util.List;

public record AuthorityBriefResponse(
        String eventId,
        String summary,
        List<String> keyEvidence,
        String recommendedVerification,
        String uncertainty,
        String status,
        Instant generatedAt
) {}