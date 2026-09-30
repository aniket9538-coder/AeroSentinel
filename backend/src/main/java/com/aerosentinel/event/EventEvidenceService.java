package com.aerosentinel.event;

import com.aerosentinel.dto.event.AttachEvidenceRequest;
import com.aerosentinel.dto.event.EventEvidenceDto;
import com.aerosentinel.exception.ResourceNotFoundException;
import com.aerosentinel.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EventEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EventEvidenceService.class);

    private final PollutionEventRepository pollutionEventRepository;
    private final EventEvidenceRepository eventEvidenceRepository;
    private final PollutionEventMapper eventMapper;

    public EventEvidenceService(
            PollutionEventRepository pollutionEventRepository,
            EventEvidenceRepository eventEvidenceRepository,
            PollutionEventMapper eventMapper
    ) {
        this.pollutionEventRepository = pollutionEventRepository;
        this.eventEvidenceRepository = eventEvidenceRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public EventEvidenceDto attachEvidence(String eventId, AttachEvidenceRequest request) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Pollution event not found: " + eventId));

        EvidenceType evidenceType;
        try {
            evidenceType = EvidenceType.valueOf(request.evidenceType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_EVIDENCE_TYPE: Supported types are AIR, WEATHER, FIRE, SATELLITE, CITIZEN, FORECAST");
        }

        EventEvidence evidence = new EventEvidence();
        evidence.setPollutionEvent(event);
        evidence.setEvidenceType(evidenceType);
        evidence.setSourceId(request.sourceId());
        evidence.setValueSummary(request.valueSummary());
        evidence.setStrength(request.strength() != null ? request.strength() : 1.0);
        evidence.setObservedAt(request.observedAt() != null ? request.observedAt() : Instant.now());
        evidence.setCreatedAt(Instant.now());

        EventEvidence saved = eventEvidenceRepository.save(evidence);
        log.info("EVENT_EVIDENCE_ATTACHED eventId={} type={} sourceId={}", eventId, evidenceType, request.sourceId());

        return eventMapper.toEvidenceDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EventEvidenceDto> getEvidencesForEvent(String eventId) {
        PollutionEvent event = pollutionEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Pollution event not found: " + eventId));

        return eventEvidenceRepository.findByPollutionEventIdOrderByObservedAtAsc(event.getId())
                .stream()
                .map(eventMapper::toEvidenceDto)
                .toList();
    }
}