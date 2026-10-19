package io.github.oumaimacoco.helpdesk.ticket;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketOrderByIdAsc(Ticket ticket);

    Optional<TicketAttachment> findByIdAndTicket(Long id, Ticket ticket);
}