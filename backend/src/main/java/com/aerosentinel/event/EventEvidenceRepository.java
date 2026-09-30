package com.aerosentinel.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventEvidenceRepository extends JpaRepository<EventEvidence, UUID> {
    List<EventEvidence> findByPollutionEventIdOrderByObservedAtAsc(UUID pollutionEventId);
    List<EventEvidence> findByEventIdOrderByObservedAtAsc(String eventId);
    List<EventEvidence> findByEventIdAndEvidenceType(String eventId, EvidenceType evidenceType);
}