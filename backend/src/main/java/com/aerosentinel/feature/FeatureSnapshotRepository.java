package com.aerosentinel.feature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureSnapshotRepository extends JpaRepository<FeatureSnapshot, UUID> {

    Optional<FeatureSnapshot> findByH3IndexAndObservedAtAndFeatureSchemaVersion(
            String h3Index, Instant observedAt, String featureSchemaVersion);

    List<FeatureSnapshot> findByH3IndexOrderByObservedAtDesc(String h3Index);

    Optional<FeatureSnapshot> findTopByH3IndexOrderByObservedAtDesc(String h3Index);

    List<FeatureSnapshot> findByCityIdAndObservedAt(UUID cityId, Instant observedAt);

    long countByQualityStatus(String qualityStatus);
}
