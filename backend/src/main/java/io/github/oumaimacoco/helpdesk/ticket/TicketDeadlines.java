package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.calendar.WorkingCalendar;
import io.github.oumaimacoco.helpdesk.calendar.WorkingCalendarProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class TicketDeadlines {

    static final Duration RESOLUTION_TIME = Duration.ofDays(3);
    static final Duration PICKUP_TIME = Duration.ofDays(7);

    private final WorkingCalendarProvider calendars;
    private final Clock clock;

    public TicketDeadlines(WorkingCalendarProvider calendars, Clock clock) {
        this.calendars = calendars;
        this.clock = clock;
    }

    void update(Ticket ticket, TicketStatus fromStatus) {
        Instant now = clock.instant();
        WorkingCalendar calendar = calendars.current();
        switch (ticket.getStatus()) {
            case NEW -> ticket.scheduleDeadlines(calendar.addWorkingTime(now, PICKUP_TIME), null, null);
            case ASSIGNED -> {
                if (fromStatus == TicketStatus.NEW) {
                    ticket.scheduleDeadlines(null, calendar.addWorkingTime(now, RESOLUTION_TIME), null);
                }
            }
            case IN_PROGRESS -> {
                if (fromStatus == TicketStatus.WAITING_FOR_USER) {
                    Duration remaining = ticket.getPausedRemaining() == null
                            ? RESOLUTION_TIME
                            : ticket.getPausedRemaining();
                    ticket.scheduleDeadlines(null, calendar.addWorkingTime(now, remaining), null);
                } else if (fromStatus == TicketStatus.RESOLVED) {
                    ticket.scheduleDeadlines(null, calendar.addWorkingTime(now, RESOLUTION_TIME), null);
                }
            }
            case WAITING_FOR_USER -> ticket.scheduleDeadlines(
                    null, null, remainingTime(calendar, ticket.getResolutionDueAt(), now));
            case RESOLVED, CLOSED -> ticket.scheduleDeadlines(null, null, null);
        }
    }

    private static Duration remainingTime(WorkingCalendar calendar, Instant dueAt, Instant now) {
        if (dueAt == null) {
            return RESOLUTION_TIME;
        }
        if (!dueAt.isAfter(now)) {
            return Duration.ZERO;
        }
        return calendar.workingTimeBetween(now, dueAt);
    }
}