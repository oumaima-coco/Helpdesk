package io.github.oumaimacoco.helpdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.oumaimacoco.helpdesk.calendar.WorkingCalendar;
import io.github.oumaimacoco.helpdesk.calendar.WorkingCalendarProvider;
import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketDeadlinesTest {

    private final WorkingCalendarProvider calendars = mock(WorkingCalendarProvider.class);

    private AppUser agent;
    private Ticket ticket;

    @BeforeEach
    void createNewTicket() {
        when(calendars.current()).thenReturn(new WorkingCalendar(List.of()));
        Team team = new Team("Support Desk");
        Category category = new Category("Imprimantes", "الطابعات", "Printers", team);
        AppUser requester = new AppUser(UUID.randomUUID(), "requester@example.com", "Demo Requester", "fr");
        agent = new AppUser(UUID.randomUUID(), "agent@example.com", "Demo Agent", "fr");
        ticket = new Ticket("HD-2026-0001", "Printer jam", "It jams", Priority.P3, category, requester);
    }

    @Test
    void newTicketMustBePickedUpWithinSevenWorkingDays() {
        deadlinesAt("2026-10-22T10:00").update(ticket, null);

        assertThat(ticket.getPickupDueAt()).isEqualTo(at("2026-11-02T10:00"));
        assertThat(ticket.getResolutionDueAt()).isNull();
    }

    @Test
    void takingTheTicketStartsThreeWorkingDays() {
        ticket.assignTo(agent);
        deadlinesAt("2026-10-22T10:00").update(ticket, TicketStatus.NEW);

        assertThat(ticket.getResolutionDueAt()).isEqualTo(at("2026-10-27T10:00"));
        assertThat(ticket.getPickupDueAt()).isNull();
    }

    @Test
    void waitingForTheRequesterPausesTheClockAndTheReplyResumesIt() {
        ticket.assignTo(agent);
        deadlinesAt("2026-10-22T10:00").update(ticket, TicketStatus.NEW);
        ticket.start();
        deadlinesAt("2026-10-22T11:00").update(ticket, TicketStatus.ASSIGNED);

        ticket.requestInfo();
        deadlinesAt("2026-10-23T10:00").update(ticket, TicketStatus.IN_PROGRESS);

        assertThat(ticket.getResolutionDueAt()).isNull();
        assertThat(ticket.getPausedRemaining()).isEqualTo(Duration.ofHours(48));

        ticket.provideInfo();
        deadlinesAt("2026-10-28T10:00").update(ticket, TicketStatus.WAITING_FOR_USER);

        assertThat(ticket.getResolutionDueAt()).isEqualTo(at("2026-10-30T10:00"));
        assertThat(ticket.getPausedRemaining()).isNull();
    }

    @Test
    void resolvingClearsEveryDeadline() {
        ticket.assignTo(agent);
        deadlinesAt("2026-10-22T10:00").update(ticket, TicketStatus.NEW);
        ticket.start();
        ticket.resolve("Replaced the roller");
        deadlinesAt("2026-10-23T10:00").update(ticket, TicketStatus.IN_PROGRESS);

        assertThat(ticket.getPickupDueAt()).isNull();
        assertThat(ticket.getResolutionDueAt()).isNull();
        assertThat(ticket.getPausedRemaining()).isNull();
    }

    @Test
    void escalationSendsTheTicketBackWithAFreshPickupDeadline() {
        ticket.assignTo(agent);
        deadlinesAt("2026-10-22T10:00").update(ticket, TicketStatus.NEW);
        ticket.escalate("Needs the network team");
        deadlinesAt("2026-10-23T10:00").update(ticket, TicketStatus.ASSIGNED);

        assertThat(ticket.getPickupDueAt()).isEqualTo(at("2026-11-03T10:00"));
        assertThat(ticket.getResolutionDueAt()).isNull();
    }

    private TicketDeadlines deadlinesAt(String localDateTime) {
        return new TicketDeadlines(calendars, Clock.fixed(at(localDateTime), ZoneOffset.UTC));
    }

    private static Instant at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(WorkingCalendar.TUNIS).toInstant();
    }
}