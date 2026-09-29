package com.aerosentinel.inspection;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_teams")
public class FieldTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_code", nullable = false, unique = true, length = 50)
    private String teamCode;

    @Column(name = "team_name", nullable = false, length = 150)
    private String teamName;

    @Column(name = "city_id")
    private UUID cityId;

    @Column(length = 30)
    private String status = "AVAILABLE";

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "leader_name", length = 100)
    private String leaderName;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public FieldTeam() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public UUID getCityId() { return cityId; }
    public void setCityId(UUID cityId) { this.cityId = cityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
