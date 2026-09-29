package com.aerosentinel.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    List<Inspection> findByAlertId(UUID alertId);

    List<Inspection> findByAlertIdOrderByAssignedAtDesc(UUID alertId);

    @Query("SELECT i FROM Inspection i WHERE i.alertId = :alertId AND i.status IN ('SCHEDULED', 'ASSIGNED', 'IN_PROGRESS')")
    Optional<Inspection> findActiveByAlertId(@Param("alertId") UUID alertId);

    List<Inspection> findByTeamId(UUID teamId);

    List<Inspection> findByStatus(String status);

    List<Inspection> findByH3Index(String h3Index);
}
