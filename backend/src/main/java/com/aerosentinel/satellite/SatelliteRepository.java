package com.aerosentinel.satellite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SatelliteRepository extends JpaRepository<SatelliteObservation, UUID> {
    List<SatelliteObservation> findByCityIdOrderByObservedAtDesc(UUID cityId);
    List<SatelliteObservation> findByH3IndexOrderByObservedAtDesc(String h3Index);
}
