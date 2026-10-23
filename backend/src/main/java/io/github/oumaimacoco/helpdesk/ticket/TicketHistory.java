package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.user.AppUser;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class TicketHistory {

    private final TicketEventRepository events;
    private final ApplicationEventPublisher publisher;

    public TicketHistory(TicketEventRepository events, ApplicationEventPublisher publisher) {
        this.events = events;
        this.publisher = publisher;
    }

    void record(Ticket ticket, AppUser actor, TicketEventType type, TicketStatus fromStatus, String detail) {
        record(ticket, actor, type, fromStatus, detail, null);
    }

    void record(Ticket ticket, AppUser actor, TicketEventType type, TicketStatus fromStatus, String detail,
                AppUser previousAssignee) {
        events.save(new TicketEvent(ticket, actor, type, fromStatus, ticket.getStatus(), detail));
        publisher.publishEvent(new TicketChanged(ticket, type, actor, previousAssignee));
    }

    List<EventResponse> historyOf(Ticket ticket, boolean showInternalDetails) {
        return events.findByTicketOrderByIdAsc(ticket).stream()
                .map(event -> EventResponse.from(event, showInternalDetails))
                .toList();
    }
}