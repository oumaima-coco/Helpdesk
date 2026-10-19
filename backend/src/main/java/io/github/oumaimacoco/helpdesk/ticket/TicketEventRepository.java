package io.github.oumaimacoco.helpdesk.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEvent, Long> {

    List<TicketEvent> findByTicketOrderByIdAsc(Ticket ticket);
}