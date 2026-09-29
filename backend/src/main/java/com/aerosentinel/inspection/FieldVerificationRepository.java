package com.aerosentinel.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldVerificationRepository extends JpaRepository<FieldVerification, UUID> {

    List<FieldVerification> findByInspectionId(UUID inspectionId);

    List<FieldVerification> findByAlertId(UUID alertId);

    List<FieldVerification> findByAlertIdOrderByInspectedAtDesc(UUID alertId);

    List<FieldVerification> findByH3Index(String h3Index);
}
