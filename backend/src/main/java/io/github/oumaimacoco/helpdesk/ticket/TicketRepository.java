package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT nextval('ticket_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();

    List<Ticket> findByRequesterOrderByCreatedAtDesc(AppUser requester);

    List<Ticket> findByAssigneeOrderByCreatedAtDesc(AppUser assignee);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.category.team = :team OR t.assignee = :agent OR t.requester = :agent
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> findVisibleToAgent(Team team, AppUser agent);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByStatusInAndResolutionDueAtBefore(Collection<TicketStatus> statuses, Instant moment);

    List<Ticket> findByStatusAndPickupDueAtBeforeAndPickupAlertedAtIsNull(TicketStatus status, Instant moment);
}