package com.aerosentinel.action;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionRepository extends JpaRepository<AuthorityAction, UUID> {
    List<AuthorityAction> findByAlertIdOrderByPerformedAtDesc(UUID alertId);
}
