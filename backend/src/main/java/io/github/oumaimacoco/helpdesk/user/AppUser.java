package io.github.oumaimacoco.helpdesk.user;

import io.github.oumaimacoco.helpdesk.common.InvalidRequestException;
import io.github.oumaimacoco.helpdesk.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("fr", "ar", "en");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", nullable = false, unique = true, updatable = false)
    private UUID keycloakId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "preferred_language", nullable = false, length = 2)
    private String preferredLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "team_lead", nullable = false)
    private boolean teamLead;

    protected AppUser() {
    }

    public AppUser(UUID keycloakId, String email, String fullName, String preferredLanguage) {
        this.keycloakId = keycloakId;
        this.email = email;
        this.fullName = fullName;
        this.preferredLanguage = preferredLanguage;
    }

    public void updateProfile(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
    }

    public void changeLanguage(String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new InvalidRequestException("Unsupported language: " + language);
        }
        this.preferredLanguage = language;
    }

    public void joinTeam(Team team) {
        this.team = team;
    }

    public void syncTeamLead(boolean teamLead) {
        this.teamLead = teamLead;
    }

    public Long getId() {
        return id;
    }

    public UUID getKeycloakId() {
        return keycloakId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public Team getTeam() {
        return team;
    }

    public boolean isTeamLead() {
        return teamLead;
    }
}