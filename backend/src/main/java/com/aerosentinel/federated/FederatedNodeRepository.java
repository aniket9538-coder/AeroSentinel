package com.aerosentinel.federated;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FederatedNodeRepository extends JpaRepository<FederatedNode, UUID> {
    Optional<FederatedNode> findByNodeId(String nodeId);
    List<FederatedNode> findAllByOrderByNodeIdAsc();
    List<FederatedNode> findByStatus(NodeStatus status);
    boolean existsByNodeId(String nodeId);
}
