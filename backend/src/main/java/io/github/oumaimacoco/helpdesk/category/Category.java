package io.github.oumaimacoco.helpdesk.category;

import io.github.oumaimacoco.helpdesk.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_fr", nullable = false, length = 100)
    private String nameFr;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    protected Category() {
        // required by JPA
    }

    public Category(String nameFr, String nameAr, String nameEn, Team team) {
        this.nameFr = nameFr;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.team = team;
    }

    public String nameIn(String language) {
        return switch (language) {
            case "ar" -> nameAr;
            case "en" -> nameEn;
            default -> nameFr;
        };
    }

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }
}