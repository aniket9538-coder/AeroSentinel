package com.aerosentinel.evidence;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;

    public EvidenceService(EvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    public List<EventEvidence> getEvidenceByEvent(UUID eventId) {
        return evidenceRepository.findByEventId(eventId);
    }

    public EventEvidence saveEvidence(EventEvidence evidence) {
        return evidenceRepository.save(evidence);
    }
}
