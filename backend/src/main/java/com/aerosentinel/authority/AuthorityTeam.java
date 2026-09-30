package com.aerosentinel.authority;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "authority_teams")
public class AuthorityTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_code", nullable = false, unique = true)
    private String teamCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "department", nullable = false)
    private String department;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }

    // Alias for getTeamCode / setTeamCode to satisfy AssignmentService
    public String getTeamId() { return teamCode; }
    public void setTeamId(String teamId) { this.teamCode = teamId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Alias for getIsActive to satisfy AssignmentService
    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { this.isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}