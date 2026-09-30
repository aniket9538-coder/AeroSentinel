package com.aerosentinel.federated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FederatedGlobalModelRepository extends JpaRepository<FederatedGlobalModel, String> {
    Optional<FederatedGlobalModel> findByIsActiveTrue();
    List<FederatedGlobalModel> findAllByOrderByCreatedAtDesc();
    boolean existsByVersion(String version);

    default Optional<FederatedGlobalModel> findByVersion(String version) {
        return findById(version);
    }

    default Optional<FederatedGlobalModel> findByModelVersion(String modelVersion) {
        return findById(modelVersion);
    }
}
