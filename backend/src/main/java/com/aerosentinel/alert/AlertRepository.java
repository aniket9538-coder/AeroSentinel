package com.aerosentinel.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Optional<Alert> findByEventId(UUID eventId);
    List<Alert> findByCityIdAndStatusOrderByCreatedAtDesc(UUID cityId, String status);
    List<Alert> findByStatusOrderByCreatedAtDesc(String status);
    List<Alert> findAllByOrderByCreatedAtDesc();
    List<Alert> findByCityIdOrderByCreatedAtDesc(UUID cityId);
    List<Alert> findByH3IndexOrderByCreatedAtDesc(String h3Index);
    List<Alert> findByTriageStateOrderByCreatedAtDesc(String triageState);
}
