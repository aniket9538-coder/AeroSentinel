package com.aerosentinel.sensor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SensorService {

    private final SensorRepository sensorRepository;

    public SensorService(SensorRepository sensorRepository) {
        this.sensorRepository = sensorRepository;
    }

    public List<MonitoringStation> getStationsByCity(UUID cityId) {
        return sensorRepository.findByCityId(cityId);
    }

    public List<MonitoringStation> getAllStations() {
        return sensorRepository.findAll();
    }
}
