package io.github.oumaimacoco.helpdesk.user;

import jakarta.validation.constraints.NotNull;

public record AssignTeamRequest(@NotNull Long teamId) {
}