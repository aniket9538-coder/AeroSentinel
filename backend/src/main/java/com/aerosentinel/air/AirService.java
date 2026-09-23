package com.aerosentinel.air;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AirService {

    private final AirObservationRepository airObservationRepository;

    public AirService(AirObservationRepository airObservationRepository) {
        this.airObservationRepository = airObservationRepository;
    }

    public List<AirObservation> getCurrentAirObservations(UUID cityId) {
        return airObservationRepository.findByCityIdOrderByObservedAtDesc(cityId);
    }

    public List<AirObservation> getStationObservations(String stationId) {
        return airObservationRepository.findByStationIdOrderByObservedAtDesc(stationId);
    }

    public AirObservation saveObservation(AirObservation observation) {
        return airObservationRepository.save(observation);
    }
}
