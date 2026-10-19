package io.github.oumaimacoco.helpdesk.ticket;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String reference,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        Long categoryId,
        String categoryName,
        String requesterName,
        String assigneeName,
        String resolution,
        String escalationReason,
        Instant createdAt,
        Instant updatedAt) {

    static TicketResponse from(Ticket ticket, String language) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory().getId(),
                ticket.getCategory().nameIn(language),
                ticket.getRequester().getFullName(),
                ticket.getAssignee() == null ? null : ticket.getAssignee().getFullName(),
                ticket.getResolution(),
                ticket.getEscalationReason(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}