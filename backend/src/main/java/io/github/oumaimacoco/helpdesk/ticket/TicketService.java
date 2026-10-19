package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.category.CategoryRepository;
import io.github.oumaimacoco.helpdesk.common.BusinessRuleException;
import io.github.oumaimacoco.helpdesk.common.NotFoundException;
import io.github.oumaimacoco.helpdesk.user.Actor;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import io.github.oumaimacoco.helpdesk.user.AppUserRepository;
import io.github.oumaimacoco.helpdesk.user.Role;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final AppUserRepository users;

    public TicketService(TicketRepository tickets, CategoryRepository categories, AppUserRepository users) {
        this.tickets = tickets;
        this.categories = categories;
        this.users = users;
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

    @Transactional
    public TicketResponse take(Actor actor, Long id) {
        Ticket ticket = findVisible(actor, id);
        if (!actor.isStaff() || !belongsToTicketTeam(actor.user(), ticket)) {
            throw new AccessDeniedException("Only agents of the ticket's team can take it");
        }
        if (ticket.getStatus() != TicketStatus.NEW) {
            throw new InvalidTicketTransitionException("Only new tickets can be taken");
        }
        ticket.assignTo(users.getReferenceById(actor.user().getId()));
        return saveAndRespond(ticket, actor);
    }

    @Transactional
    public TicketResponse assign(Actor actor, Long id, Long agentId) {
        Ticket ticket = findVisible(actor, id);
        if (!actor.has(Role.TEAM_LEAD) || !belongsToTicketTeam(actor.user(), ticket)) {
            throw new AccessDeniedException("Only the team lead of the ticket's team can assign it");
        }
        AppUser agent = users.findById(agentId)
                .orElseThrow(() -> new NotFoundException("User " + agentId + " not found"));
        if (!belongsToTicketTeam(agent, ticket)) {
            throw new BusinessRuleException("The agent must belong to the ticket's team");
        }
        ticket.assignTo(agent);
        return saveAndRespond(ticket, actor);
    }

    @Transactional
    public TicketResponse start(Actor actor, Long id) {
        return asAssignee(actor, id, Ticket::start);
    }

    @Transactional
    public TicketResponse requestInfo(Actor actor, Long id) {
        return asAssignee(actor, id, Ticket::requestInfo);
    }

    @Transactional
    public TicketResponse resolve(Actor actor, Long id, String resolution) {
        return asAssignee(actor, id, ticket -> ticket.resolve(resolution));
    }

    @Transactional
    public TicketResponse escalate(Actor actor, Long id, String reason) {
        return asAssignee(actor, id, ticket -> ticket.escalate(reason));
    }

    @Transactional
    public TicketResponse provideInfo(Actor actor, Long id) {
        return asRequester(actor, id, Ticket::provideInfo);
    }

    @Transactional
    public TicketResponse confirm(Actor actor, Long id) {
        return asRequester(actor, id, Ticket::confirm);
    }

    @Transactional
    public TicketResponse reopen(Actor actor, Long id) {
        return asRequester(actor, id, Ticket::reopen);
    }

    @Transactional
    public TicketResponse cancel(Actor actor, Long id) {
        return asRequester(actor, id, Ticket::cancel);
    }

    Ticket findVisible(Actor actor, Long id) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> ticketNotFound(id));
        if (!canSee(actor, ticket)) {
            throw ticketNotFound(id);
        }
        return ticket;
    }

    private TicketResponse asAssignee(Actor actor, Long id, Consumer<Ticket> action) {
        Ticket ticket = findVisible(actor, id);
        if (!ticket.isAssignedTo(actor.user())) {
            throw new AccessDeniedException("Only the assigned agent can do this");
        }
        action.accept(ticket);
        return saveAndRespond(ticket, actor);
    }

    private TicketResponse asRequester(Actor actor, Long id, Consumer<Ticket> action) {
        Ticket ticket = findVisible(actor, id);
        if (!ticket.isRequestedBy(actor.user())) {
            throw new AccessDeniedException("Only the requester can do this");
        }
        action.accept(ticket);
        return saveAndRespond(ticket, actor);
    }

    private TicketResponse saveAndRespond(Ticket ticket, Actor actor) {
        tickets.flush();
        return toResponse(ticket, actor);
    }

    private boolean canSee(Actor actor, Ticket ticket) {
        if (actor.has(Role.ADMIN) || ticket.isRequestedBy(actor.user()) || ticket.isAssignedTo(actor.user())) {
            return true;
        }
        return actor.isStaff() && belongsToTicketTeam(actor.user(), ticket);
    }

    private static boolean belongsToTicketTeam(AppUser user, Ticket ticket) {
        return user.getTeam() != null
                && user.getTeam().getId().equals(ticket.getCategory().getTeam().getId());
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