package com.aerosentinel.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<EventEvidence, UUID> {
    List<EventEvidence> findByEventId(UUID eventId);
}
