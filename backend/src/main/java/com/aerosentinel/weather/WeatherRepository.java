package com.aerosentinel.weather;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeatherRepository extends JpaRepository<WeatherObservation, UUID> {
    List<WeatherObservation> findByCityIdOrderByObservedAtDesc(UUID cityId);
    Optional<WeatherObservation> findFirstByCityIdOrderByObservedAtDesc(UUID cityId);
}
