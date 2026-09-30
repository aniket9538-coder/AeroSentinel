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
    Optional<PollutionEvent> findTopByH3IndexOrderByStartedAtDesc(String h3Index);
    List<PollutionEvent> findByH3IndexOrderByStartedAtDesc(String h3Index);
    Optional<PollutionEvent> findByPredictionId(UUID predictionId);
    List<PollutionEvent> findAllByOrderByStartedAtDesc();
    default java.util.Optional<PollutionEvent> findByEventId(String eventId) {
        return findByEventCode(eventId);
    }
}
