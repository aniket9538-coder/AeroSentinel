package com.aerosentinel.dto.alert;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Clean, safe client-facing DTO representing a citizen evidence observation
 * attached to a PollutionEvent or Authority Alert candidate.
 * Never exposes unrestricted local filesystem paths.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CitizenEvidenceItemDto(
        UUID reportId,
        String reportReference,
        String h3Index,
        String category,
        String description,
        Instant observedAt,
        String visibleCondition,
        Double visualConfidence,
        List<String> visualObservations,
        List<String> visualUncertainty,
        String photoUrl,
        String dataSource,
        String relevanceTier,
        UUID eventId,
        String evidenceKey
) {}
