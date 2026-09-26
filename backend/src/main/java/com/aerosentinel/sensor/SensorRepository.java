package com.aerosentinel.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SensorRepository extends JpaRepository<MonitoringStation, UUID> {
    List<MonitoringStation> findByCityId(UUID cityId);
    List<MonitoringStation> findByCityIdAndStatus(UUID cityId, String status);
    Optional<MonitoringStation> findByStationCode(String stationCode);
    boolean existsByStationCode(String stationCode);
}
