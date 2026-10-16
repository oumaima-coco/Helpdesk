package io.github.oumaimacoco.helpdesk.user;

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

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "preferred_language", nullable = false, length = 2)
    private String preferredLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    protected AppUser() {
        // required by JPA
    }

    public AppUser(String email, String fullName, String preferredLanguage) {
        this.email = email;
        this.fullName = fullName;
        this.preferredLanguage = preferredLanguage;
    }

    public void joinTeam(Team team) {
        this.team = team;
    }

    public Long getId() {
        return id;
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
}