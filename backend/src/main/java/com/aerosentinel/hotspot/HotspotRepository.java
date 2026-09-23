package com.aerosentinel.hotspot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HotspotRepository extends JpaRepository<HotspotPrediction, UUID> {
    List<HotspotPrediction> findByGridCellIdOrderByPredictedAtDesc(UUID gridCellId);
    List<HotspotPrediction> findByRiskLevel(String riskLevel);
}
