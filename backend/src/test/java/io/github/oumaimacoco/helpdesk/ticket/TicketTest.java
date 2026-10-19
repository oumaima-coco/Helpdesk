package io.github.oumaimacoco.helpdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketTest {

    private AppUser agent;
    private AppUser otherAgent;
    private Ticket ticket;

    @BeforeEach
    void createNewTicket() {
        Team team = new Team("Support Desk");
        Category category = new Category("Imprimantes", "الطابعات", "Printers", team);
        AppUser requester = new AppUser(UUID.randomUUID(), "requester@example.com", "Demo Requester", "fr");
        agent = new AppUser(UUID.randomUUID(), "agent@example.com", "Demo Agent", "fr");
        otherAgent = new AppUser(UUID.randomUUID(), "agent2@example.com", "Second Agent", "fr");
        ticket = new Ticket("HD-2026-0001", "Printer jam", "The printer on floor 2 jams", Priority.P3, category, requester);
    }

    @Test
    void newTicketStartsAsNewWithoutAssignee() {
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getAssignee()).isNull();
    }

    @Test
    void fullWorkflowEndsClosedWithItsResolution() {
        ticket.assignTo(agent);
        ticket.start();
        ticket.requestInfo();
        ticket.provideInfo();
        ticket.resolve("Replaced the worn roller");
        ticket.confirm();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getResolution()).isEqualTo("Replaced the worn roller");
    }

    @Test
    void cannotResolveATicketNobodyWorkedOn() {
        assertThatThrownBy(() -> ticket.resolve("Fixed"))
                .isInstanceOf(InvalidTicketTransitionException.class)
                .hasMessage("Cannot resolve a ticket in status NEW");
    }

    @Test
    void cannotResolveWithABlankResolution() {
        ticket.assignTo(agent);
        ticket.start();

        assertThatThrownBy(() -> ticket.resolve("   "))
                .isInstanceOf(InvalidTicketTransitionException.class)
                .hasMessage("A resolution is required to resolve a ticket");
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void resolutionIsStoredWithoutSurroundingSpaces() {
        ticket.assignTo(agent);
        ticket.start();
        ticket.resolve("  Restarted the print spooler  ");

        assertThat(ticket.getResolution()).isEqualTo("Restarted the print spooler");
    }

    @Test
    void teamLeadCanReassignAnAssignedTicket() {
        ticket.assignTo(agent);
        ticket.assignTo(otherAgent);

        assertThat(ticket.getAssignee()).isSameAs(otherAgent);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
    }

    @Test
    void escalationReturnsTheTicketToTheQueueWithItsReason() {
        ticket.assignTo(agent);
        ticket.start();
        ticket.escalate("Needs network access I do not have");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getAssignee()).isNull();
        assertThat(ticket.getEscalationReason()).isEqualTo("Needs network access I do not have");
    }

    @Test
    void escalationRequiresAReason() {
        ticket.assignTo(agent);

        assertThatThrownBy(() -> ticket.escalate(""))
                .isInstanceOf(InvalidTicketTransitionException.class)
                .hasMessage("A reason is required to escalate a ticket");
        assertThat(ticket.getAssignee()).isSameAs(agent);
    }

    @Test
    void requesterCanReopenAResolvedTicket() {
        ticket.assignTo(agent);
        ticket.start();
        ticket.resolve("Cleared the cache");
        ticket.reopen();

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void onlyANewTicketCanBeCancelled() {
        ticket.assignTo(agent);

        assertThatThrownBy(() -> ticket.cancel())
                .isInstanceOf(InvalidTicketTransitionException.class)
                .hasMessage("Cannot cancel a ticket in status ASSIGNED");
    }

    @Test
    void aClosedTicketCannotChangeAnymore() {
        ticket.cancel();

        assertThatThrownBy(() -> ticket.assignTo(agent)).isInstanceOf(InvalidTicketTransitionException.class);
        assertThatThrownBy(() -> ticket.reopen()).isInstanceOf(InvalidTicketTransitionException.class);
        assertThatThrownBy(() -> ticket.start()).isInstanceOf(InvalidTicketTransitionException.class);
    }
}