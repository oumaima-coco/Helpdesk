package io.github.oumaimacoco.helpdesk.user;

public record UserSummary(Long id, String email, String fullName, Long teamId, String teamName) {

    static UserSummary from(AppUser user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getTeam() == null ? null : user.getTeam().getId(),
                user.getTeam() == null ? null : user.getTeam().getName());
    }
}