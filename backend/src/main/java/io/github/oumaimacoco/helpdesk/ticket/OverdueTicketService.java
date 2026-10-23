package io.github.oumaimacoco.helpdesk.ticket;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OverdueTicketService {

    private static final List<TicketStatus> RUNNING = List.of(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);

    private final TicketRepository tickets;
    private final TicketHistory ticketHistory;
    private final TicketDeadlines deadlines;
    private final Clock clock;

    public OverdueTicketService(TicketRepository tickets, TicketHistory ticketHistory,
                                TicketDeadlines deadlines, Clock clock) {
        this.tickets = tickets;
        this.ticketHistory = ticketHistory;
        this.deadlines = deadlines;
        this.clock = clock;
    }

    @Transactional
    public int returnOverdueTickets() {
        List<Ticket> overdue = tickets.findByStatusInAndResolutionDueAtBefore(RUNNING, clock.instant());
        for (Ticket ticket : overdue) {
            TicketStatus fromStatus = ticket.getStatus();
            String previousAgent = ticket.getAssignee() == null ? null : ticket.getAssignee().getFullName();
            ticket.returnToQueue();
            deadlines.update(ticket, fromStatus);
            ticketHistory.record(ticket, null, TicketEventType.OVERDUE, fromStatus, previousAgent);
        }
        return overdue.size();
    }

    @Transactional
    public int flagTicketsNotPickedUp() {
        Instant now = clock.instant();
        List<Ticket> waiting = tickets.findByStatusAndPickupDueAtBeforeAndPickupAlertedAtIsNull(TicketStatus.NEW, now);
        for (Ticket ticket : waiting) {
            ticket.markPickupAlerted(now);
            ticketHistory.record(ticket, null, TicketEventType.PICKUP_OVERDUE, TicketStatus.NEW, null);
        }
        return waiting.size();
    }
}