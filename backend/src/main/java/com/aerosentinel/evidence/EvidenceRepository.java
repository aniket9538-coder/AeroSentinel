package com.aerosentinel.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<EventEvidence, UUID> {
    List<EventEvidence> findByEventId(UUID eventId);
    List<EventEvidence> findByEventIdOrderByCreatedAtAsc(UUID eventId);
    Optional<EventEvidence> findByEventIdAndEvidenceKey(UUID eventId, String evidenceKey);
    boolean existsByEventIdAndEvidenceKey(UUID eventId, String evidenceKey);
    void deleteByEventId(UUID eventId);
}
