package io.github.oumaimacoco.helpdesk.ticket;

import java.time.Instant;

public record EventResponse(
        Long id,
        TicketEventType type,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String actorName,
        String detail,
        Instant createdAt) {

    static EventResponse from(TicketEvent event, boolean showInternalDetails) {
        boolean hideDetail = event.getType() == TicketEventType.ESCALATED && !showInternalDetails;
        return new EventResponse(
                event.getId(),
                event.getType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getActor() == null ? null : event.getActor().getFullName(),
                hideDetail ? null : event.getDetail(),
                event.getCreatedAt());
    }
}