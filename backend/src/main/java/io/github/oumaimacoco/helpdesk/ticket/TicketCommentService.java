package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.common.BusinessRuleException;
import io.github.oumaimacoco.helpdesk.user.Actor;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import io.github.oumaimacoco.helpdesk.user.AppUserRepository;
import io.github.oumaimacoco.helpdesk.user.Role;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketCommentService {

    private final TicketService ticketService;
    private final TicketCommentRepository comments;
    private final AppUserRepository users;

    public TicketCommentService(TicketService ticketService, TicketCommentRepository comments,
                                AppUserRepository users) {
        this.ticketService = ticketService;
        this.comments = comments;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Actor actor, Long ticketId) {
        Ticket ticket = ticketService.findVisible(actor, ticketId);
        List<TicketComment> found = canSeeInternalNotes(actor)
                ? comments.findByTicketOrderByCreatedAtAsc(ticket)
                : comments.findByTicketAndInternalFalseOrderByCreatedAtAsc(ticket);
        return found.stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse add(Actor actor, Long ticketId, CreateCommentRequest request) {
        Ticket ticket = ticketService.findVisible(actor, ticketId);
        boolean isRequester = ticket.isRequestedBy(actor.user());
        if (!isRequester && !actor.isStaff()) {
            throw new AccessDeniedException("Only the requester and support staff can comment");
        }
        if (request.internal() && !actor.isStaff()) {
            throw new AccessDeniedException("Only support staff can write internal notes");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Cannot comment on a closed ticket");
        }

        AppUser author = users.getReferenceById(actor.user().getId());
        TicketComment comment = comments.save(
                new TicketComment(ticket, author, request.body().strip(), request.internal()));

        if (isRequester && !request.internal() && ticket.getStatus() == TicketStatus.WAITING_FOR_USER) {
            ticket.provideInfo();
        }
        comments.flush();
        return CommentResponse.from(comment);
    }

    private static boolean canSeeInternalNotes(Actor actor) {
        return actor.isStaff() || actor.has(Role.ADMIN);
    }
}