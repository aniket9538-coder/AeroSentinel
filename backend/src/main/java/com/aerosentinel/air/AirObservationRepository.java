package com.aerosentinel.air;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AirObservationRepository extends JpaRepository<AirObservation, UUID> {
    List<AirObservation> findByCityIdOrderByObservedAtDesc(UUID cityId);
    List<AirObservation> findByStationIdOrderByObservedAtDesc(String stationId);
}
