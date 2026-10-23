package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.user.AppUser;

public record TicketChanged(Ticket ticket, TicketEventType type, AppUser actor, AppUser previousAssignee) {
}