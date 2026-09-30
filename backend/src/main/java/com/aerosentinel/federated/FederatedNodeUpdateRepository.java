package com.aerosentinel.federated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FederatedNodeUpdateRepository extends JpaRepository<FederatedNodeUpdate, UUID> {
    List<FederatedNodeUpdate> findByRoundId(String roundId);
    Optional<FederatedNodeUpdate> findByRoundIdAndCityName(String roundId, String cityName);
    boolean existsByRoundIdAndCityName(String roundId, String cityName);
    long countByRoundId(String roundId);
}
