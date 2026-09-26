package com.aerosentinel.hotspot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HotspotRepository extends JpaRepository<HotspotPrediction, UUID> {

    List<HotspotPrediction> findByCityIdOrderByPredictedAtDesc(UUID cityId);

    List<HotspotPrediction> findByH3IndexOrderByPredictedAtDesc(String h3Index);

    @Query(value = "SELECT * FROM hotspot_predictions WHERE h3_index = :h3Index ORDER BY predicted_at DESC, created_at DESC LIMIT 1", nativeQuery = true)
    Optional<HotspotPrediction> findTopByH3IndexOrderByPredictedAtDesc(@Param("h3Index") String h3Index);

    List<HotspotPrediction> findByGridCellIdOrderByPredictedAtDesc(UUID gridCellId);

    List<HotspotPrediction> findByRiskLevel(String riskLevel);

    @Query(value = "SELECT * FROM hotspot_predictions WHERE id IN (" +
            "  SELECT DISTINCT ON (h3_index) id FROM hotspot_predictions " +
            "  WHERE city_id = :cityId " +
            "  ORDER BY h3_index, predicted_at DESC, created_at DESC" +
            ") ORDER BY risk_score DESC", nativeQuery = true)
    List<HotspotPrediction> findLatestByCityId(@Param("cityId") UUID cityId);
}
