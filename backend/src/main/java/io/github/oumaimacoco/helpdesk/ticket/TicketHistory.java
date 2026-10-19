package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.user.AppUser;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketHistory {

    private final TicketEventRepository events;

    public TicketHistory(TicketEventRepository events) {
        this.events = events;
    }

    void record(Ticket ticket, AppUser actor, TicketEventType type, TicketStatus fromStatus, String detail) {
        events.save(new TicketEvent(ticket, actor, type, fromStatus, ticket.getStatus(), detail));
    }

    List<EventResponse> historyOf(Ticket ticket, boolean showInternalDetails) {
        return events.findByTicketOrderByIdAsc(ticket).stream()
                .map(event -> EventResponse.from(event, showInternalDetails))
                .toList();
    }
}