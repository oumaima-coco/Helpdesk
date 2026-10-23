package io.github.oumaimacoco.helpdesk.ticket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeadlineWatcher {

    private static final Logger log = LoggerFactory.getLogger(DeadlineWatcher.class);

    private final OverdueTicketService overdueTickets;

    public DeadlineWatcher(OverdueTicketService overdueTickets) {
        this.overdueTickets = overdueTickets;
    }

    @Scheduled(
            fixedDelayString = "${helpdesk.deadlines.check-interval:PT5M}",
            initialDelayString = "${helpdesk.deadlines.initial-delay:PT1M}")
    public void check() {
        int returned = overdueTickets.returnOverdueTickets();
        int flagged = overdueTickets.flagTicketsNotPickedUp();
        if (returned > 0 || flagged > 0) {
            log.info("Deadline check: {} overdue ticket(s) returned to the queue, {} ticket(s) not picked up in time",
                    returned, flagged);
        }
    }
}