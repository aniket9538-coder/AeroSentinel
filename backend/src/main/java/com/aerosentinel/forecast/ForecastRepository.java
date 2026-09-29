package com.aerosentinel.forecast;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, UUID> {

    List<Forecast> findByParentPredictionIdOrderByHorizonHoursAsc(UUID parentPredictionId);

    Optional<Forecast> findByParentPredictionIdAndHorizonHours(UUID parentPredictionId, Integer horizonHours);

    boolean existsByParentPredictionId(UUID parentPredictionId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByParentPredictionId(UUID parentPredictionId);

    List<Forecast> findByH3IndexOrderByGeneratedAtDesc(String h3Index);

    List<Forecast> findByCityIdOrderByGeneratedAtDesc(UUID cityId);

    @Query(value = "SELECT * FROM forecasts WHERE parent_prediction_id = (" +
            "  SELECT parent_prediction_id FROM forecasts " +
            "  WHERE h3_index = :h3Index " +
            "  ORDER BY generated_at DESC, created_at DESC LIMIT 1" +
            ") ORDER BY horizon_hours ASC", nativeQuery = true)
    List<Forecast> findLatestByH3Index(@Param("h3Index") String h3Index);

    @Query(value = "SELECT * FROM forecasts WHERE parent_prediction_id = (" +
            "  SELECT parent_prediction_id FROM forecasts " +
            "  WHERE city_id = :cityId AND h3_index = :h3Index " +
            "  ORDER BY generated_at DESC, created_at DESC LIMIT 1" +
            ") ORDER BY horizon_hours ASC", nativeQuery = true)
    List<Forecast> findLatestByCityIdAndH3Index(@Param("cityId") UUID cityId, @Param("h3Index") String h3Index);
}
