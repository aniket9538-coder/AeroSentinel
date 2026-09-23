package com.aerosentinel.fire;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FireService {

    private final FireRepository fireRepository;

    public FireService(FireRepository fireRepository) {
        this.fireRepository = fireRepository;
    }

    public List<FireEvent> getFiresByCity(UUID cityId) {
        return fireRepository.findByCityIdOrderByDetectedAtDesc(cityId);
    }

    public FireEvent saveFire(FireEvent fire) {
        return fireRepository.save(fire);
    }
}
