package com.aerosentinel.repository;

import com.aerosentinel.model.GeminiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for GeminiAnalysis entities.
 */
@Repository
public interface GeminiAnalysisRepository extends JpaRepository<GeminiAnalysis, UUID> {

    Optional<GeminiAnalysis> findTopByH3IndexOrderByCreatedAtDesc(String h3Index);

    List<GeminiAnalysis> findByH3IndexOrderByCreatedAtDesc(String h3Index);

    Optional<GeminiAnalysis> findTopByEventIdOrderByCreatedAtDesc(UUID eventId);

    List<GeminiAnalysis> findByEventIdOrderByCreatedAtDesc(UUID eventId);

    List<GeminiAnalysis> findByPredictionIdOrderByCreatedAtDesc(UUID predictionId);

    Optional<GeminiAnalysis> findTopByCitizenReportIdOrderByCreatedAtDesc(UUID citizenReportId);

    List<GeminiAnalysis> findByCitizenReportIdOrderByCreatedAtDesc(UUID citizenReportId);
}
