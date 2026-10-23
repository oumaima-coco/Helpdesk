package io.github.oumaimacoco.helpdesk.ticket;

import java.time.Instant;
import java.util.List;

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
        Instant updatedAt,
        Instant pickupDueAt,
        Instant resolutionDueAt,
        Long pausedRemainingSeconds,
        List<TicketAction> actions) {

    static TicketResponse from(Ticket ticket, String language, List<TicketAction> actions) {
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
                ticket.getUpdatedAt(),
                ticket.getPickupDueAt(),
                ticket.getResolutionDueAt(),
                ticket.getPausedRemaining() == null ? null : ticket.getPausedRemaining().toSeconds(),
                actions);
    }
}