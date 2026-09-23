package com.aerosentinel.sensor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensorRepository extends JpaRepository<MonitoringStation, UUID> {
    List<MonitoringStation> findByCityId(UUID cityId);
}
