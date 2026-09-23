package com.aerosentinel.hotspot;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HotspotService {

    private final HotspotRepository hotspotRepository;

    public HotspotService(HotspotRepository hotspotRepository) {
        this.hotspotRepository = hotspotRepository;
    }

    public List<HotspotPrediction> getHotspotsByCell(UUID cellId) {
        return hotspotRepository.findByGridCellIdOrderByPredictedAtDesc(cellId);
    }

    public List<HotspotPrediction> getAllHotspots() {
        return hotspotRepository.findAll();
    }

    public HotspotPrediction savePrediction(HotspotPrediction prediction) {
        return hotspotRepository.save(prediction);
    }
}
