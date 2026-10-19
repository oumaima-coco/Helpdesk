package io.github.oumaimacoco.helpdesk.user;

import io.github.oumaimacoco.helpdesk.common.NotFoundException;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.team.TeamRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private final AppUserRepository users;
    private final TeamRepository teams;

    public UserAdminService(AppUserRepository users, TeamRepository teams) {
        this.users = users;
        this.teams = teams;
    }

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return users.findAll(Sort.by("id")).stream().map(UserSummary::from).toList();
    }

    @Transactional
    public UserSummary assignTeam(Long userId, Long teamId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        Team team = teams.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team " + teamId + " not found"));
        user.joinTeam(team);
        return UserSummary.from(user);
    }
}
