package com.aerosentinel.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollutionEventRepository extends JpaRepository<PollutionEvent, UUID> {
    Optional<PollutionEvent> findByEventCode(String eventCode);
    List<PollutionEvent> findByStatus(String status);
}
