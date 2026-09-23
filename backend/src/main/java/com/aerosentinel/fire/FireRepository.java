package com.aerosentinel.fire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FireRepository extends JpaRepository<FireEvent, UUID> {
    List<FireEvent> findByCityIdOrderByDetectedAtDesc(UUID cityId);
}
