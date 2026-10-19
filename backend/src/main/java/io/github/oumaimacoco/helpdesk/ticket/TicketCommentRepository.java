package io.github.oumaimacoco.helpdesk.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    List<TicketComment> findByTicketAndInternalFalseOrderByCreatedAtAsc(Ticket ticket);
}