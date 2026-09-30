package com.aerosentinel.event;

import com.aerosentinel.dto.event.EventDetailResponse;
import com.aerosentinel.dto.event.EventEvidenceDto;
import com.aerosentinel.dto.event.PollutionEventResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PollutionEventMapper {

    public PollutionEventResponse toResponse(PollutionEvent event) {
        if (event == null) return null;
        return new PollutionEventResponse(
                event.getEventId(),
                event.getCityId(),
                event.getH3Index(),
                event.getStartedAt(),
                event.getEndedAt(),
                event.getRiskScore(),
                event.getRiskLevel(),
                event.getConfidence(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public EventDetailResponse toDetailResponse(PollutionEvent event, List<EventEvidence> evidences) {
        if (event == null) return null;
        List<EventEvidenceDto> evidenceDtos = evidences != null
                ? evidences.stream().map(this::toEvidenceDto).toList()
                : Collections.emptyList();

        return new EventDetailResponse(
                event.getEventId(),
                event.getCityId(),
                event.getH3Index(),
                event.getStartedAt(),
                event.getEndedAt(),
                event.getRiskScore(),
                event.getRiskLevel(),
                event.getConfidence(),
                event.getStatus(),
                evidenceDtos,
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    public EventEvidenceDto toEvidenceDto(EventEvidence evidence) {
        if (evidence == null) return null;
        return new EventEvidenceDto(
                evidence.getId(),
                evidence.getEvidenceType().name(),
                evidence.getSourceId(),
                evidence.getValueSummary(),
                evidence.getStrength(),
                evidence.getObservedAt(),
                evidence.getCreatedAt()
        );
    }
}