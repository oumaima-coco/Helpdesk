package io.github.oumaimacoco.helpdesk.ticket;

import jakarta.validation.constraints.NotNull;

public record AssignTicketRequest(@NotNull Long agentId) {
}