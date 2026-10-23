package io.github.oumaimacoco.helpdesk.user;

import io.github.oumaimacoco.helpdesk.team.Team;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByKeycloakId(UUID keycloakId);

    List<AppUser> findByTeamAndTeamLeadTrue(Team team);
}