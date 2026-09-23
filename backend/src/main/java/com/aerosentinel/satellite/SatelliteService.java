package com.aerosentinel.satellite;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SatelliteService {

    private final SatelliteRepository satelliteRepository;

    public SatelliteService(SatelliteRepository satelliteRepository) {
        this.satelliteRepository = satelliteRepository;
    }

    public List<SatelliteObservation> getObservationsByCity(UUID cityId) {
        return satelliteRepository.findByCityIdOrderByObservedAtDesc(cityId);
    }

    public List<SatelliteObservation> getObservationsByH3(String h3Index) {
        return satelliteRepository.findByH3IndexOrderByObservedAtDesc(h3Index);
    }

    public SatelliteObservation saveObservation(SatelliteObservation obs) {
        return satelliteRepository.save(obs);
    }
}
