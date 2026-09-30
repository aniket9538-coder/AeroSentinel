package com.aerosentinel.federated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FederatedRoundRepository extends JpaRepository<FederatedRound, UUID> {
    Optional<FederatedRound> findByRoundId(String roundId);
    List<FederatedRound> findAllByOrderByCreatedAtDesc();
    Optional<FederatedRound> findFirstByOrderByCreatedAtDesc();
    boolean existsByRoundId(String roundId);
}
