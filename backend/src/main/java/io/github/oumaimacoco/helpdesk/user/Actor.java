package io.github.oumaimacoco.helpdesk.user;

import java.util.Set;

public record Actor(AppUser user, Set<Role> roles) {

    public Actor {
        roles = Set.copyOf(roles);
    }

    public boolean has(Role role) {
        return roles.contains(role);
    }

    public boolean isStaff() {
        return has(Role.AGENT) || has(Role.TEAM_LEAD);
    }
}