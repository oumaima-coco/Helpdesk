package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.category.CategoryRepository;
import io.github.oumaimacoco.helpdesk.common.NotFoundException;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.user.Actor;
import io.github.oumaimacoco.helpdesk.user.Role;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final TicketRepository tickets;
    private final CategoryRepository categories;

    public TicketService(TicketRepository tickets, CategoryRepository categories) {
        this.tickets = tickets;
        this.categories = categories;
    }

    @Transactional
    public TicketResponse create(Actor actor, CreateTicketRequest request) {
        if (!actor.has(Role.REQUESTER) && !actor.isStaff()) {
            throw new AccessDeniedException("Only requesters and support staff can create tickets");
        }
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category " + request.categoryId() + " not found"));
        Ticket ticket = new Ticket(
                nextReference(),
                request.title().strip(),
                request.description().strip(),
                Priority.P3,
                category,
                actor.user());
        return toResponse(tickets.save(ticket), actor);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listVisible(Actor actor) {
        List<Ticket> found;
        if (actor.has(Role.ADMIN)) {
            found = tickets.findAllByOrderByCreatedAtDesc();
        } else if (actor.isStaff()) {
            found = tickets.findVisibleToAgent(actor.user().getTeam(), actor.user());
        } else {
            found = tickets.findByRequesterOrderByCreatedAtDesc(actor.user());
        }
        return found.stream().map(ticket -> toResponse(ticket, actor)).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listAssignedTo(Actor actor) {
        if (!actor.isStaff()) {
            throw new AccessDeniedException("Only support staff have assigned tickets");
        }
        return tickets.findByAssigneeOrderByCreatedAtDesc(actor.user()).stream()
                .map(ticket -> toResponse(ticket, actor))
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Actor actor, Long id) {
        return toResponse(findVisible(actor, id), actor);
    }

    Ticket findVisible(Actor actor, Long id) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> ticketNotFound(id));
        if (!canSee(actor, ticket)) {
            throw ticketNotFound(id);
        }
        return ticket;
    }

    private boolean canSee(Actor actor, Ticket ticket) {
        if (actor.has(Role.ADMIN) || ticket.isRequestedBy(actor.user()) || ticket.isAssignedTo(actor.user())) {
            return true;
        }
        Team team = actor.user().getTeam();
        return actor.isStaff()
                && team != null
                && ticket.getCategory().getTeam().getId().equals(team.getId());
    }

    private String nextReference() {
        long number = tickets.nextReferenceNumber();
        return "HD-%d-%04d".formatted(Year.now(TUNIS).getValue(), number);
    }

    private static TicketResponse toResponse(Ticket ticket, Actor actor) {
        return TicketResponse.from(ticket, actor.user().getPreferredLanguage());
    }

    private static NotFoundException ticketNotFound(Long id) {
        return new NotFoundException("Ticket " + id + " not found");
    }
}