package com.aerosentinel.authority;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthorityTeamRepository extends JpaRepository<AuthorityTeam, UUID> {
    Optional<AuthorityTeam> findByTeamCode(String teamCode);

    @Query("SELECT t FROM AuthorityTeam t WHERE t.teamCode = :teamId")
    Optional<AuthorityTeam> findByTeamId(@Param("teamId") String teamId);

    List<AuthorityTeam> findByIsActiveTrue();
}