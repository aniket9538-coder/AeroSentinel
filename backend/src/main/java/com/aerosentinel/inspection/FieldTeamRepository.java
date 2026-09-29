package com.aerosentinel.inspection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldTeamRepository extends JpaRepository<FieldTeam, UUID> {

    List<FieldTeam> findByCityId(UUID cityId);

    List<FieldTeam> findByStatus(String status);

    List<FieldTeam> findByCityIdAndStatus(UUID cityId, String status);

    Optional<FieldTeam> findByTeamCode(String teamCode);
}
