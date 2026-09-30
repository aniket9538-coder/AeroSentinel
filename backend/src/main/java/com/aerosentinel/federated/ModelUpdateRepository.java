package com.aerosentinel.federated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelUpdateRepository extends JpaRepository<ModelUpdate, UUID> {
    List<ModelUpdate> findByRoundId(String roundId);
    Optional<ModelUpdate> findByRoundIdAndNodeId(String roundId, String nodeId);
    boolean existsByRoundIdAndNodeId(String roundId, String nodeId);
    long countByRoundIdAndStatus(String roundId, UpdateStatus status);
}
