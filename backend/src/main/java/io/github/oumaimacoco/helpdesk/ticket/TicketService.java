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
import java.util.function.Function;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final ZoneId TUNIS = ZoneId.of("Africa/Tunis");

    private final TicketRepository tickets;
    private final CategoryRepository categories;
    private final AppUserRepository users;
    private final TicketHistory ticketHistory;

    public TicketService(TicketRepository tickets, CategoryRepository categories,
                         AppUserRepository users, TicketHistory ticketHistory) {
        this.tickets = tickets;
        this.categories = categories;
        this.users = users;
        this.ticketHistory = ticketHistory;
    }

    @Transactional
    public TicketResponse create(Actor actor, CreateTicketRequest request) {
        if (!actor.has(Role.REQUESTER) && !actor.isStaff()) {
            throw new AccessDeniedException("Only requesters and support staff can create tickets");
        }
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category " + request.categoryId() + " not found"));
        AppUser requester = userOf(actor);
        Ticket ticket = tickets.save(new Ticket(
                nextReference(),
                request.title().strip(),
                request.description().strip(),
                Priority.P3,
                category,
                requester));
        ticketHistory.record(ticket, requester, TicketEventType.CREATED, null, null);
        return saveAndRespond(ticket, actor);
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

    @Transactional(readOnly = true)
    public List<EventResponse> history(Actor actor, Long id) {
        Ticket ticket = findVisible(actor, id);
        boolean showInternalDetails = actor.isStaff() || actor.has(Role.ADMIN);
        return ticketHistory.historyOf(ticket, showInternalDetails);
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
        AppUser agent = userOf(actor);
        TicketStatus fromStatus = ticket.getStatus();
        ticket.assignTo(agent);
        ticketHistory.record(ticket, agent, TicketEventType.TAKEN, fromStatus, null);
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
        TicketStatus fromStatus = ticket.getStatus();
        ticket.assignTo(agent);
        ticketHistory.record(ticket, userOf(actor), TicketEventType.ASSIGNED, fromStatus, agent.getFullName());
        return saveAndRespond(ticket, actor);
    }

    @Transactional
    public TicketResponse start(Actor actor, Long id) {
        return asAssignee(actor, id, TicketEventType.STARTED, Ticket::start);
    }

    @Transactional
    public TicketResponse requestInfo(Actor actor, Long id) {
        return asAssignee(actor, id, TicketEventType.INFO_REQUESTED, Ticket::requestInfo);
    }

    @Transactional
    public TicketResponse resolve(Actor actor, Long id, String resolution) {
        return asAssignee(actor, id, TicketEventType.RESOLVED, ticket -> ticket.resolve(resolution));
    }

    @Transactional
    public TicketResponse escalate(Actor actor, Long id, String reason) {
        return asAssignee(actor, id, TicketEventType.ESCALATED,
                ticket -> ticket.escalate(reason), Ticket::getEscalationReason);
    }

    @Transactional
    public TicketResponse provideInfo(Actor actor, Long id) {
        return asRequester(actor, id, TicketEventType.INFO_PROVIDED, Ticket::provideInfo);
    }

    @Transactional
    public TicketResponse confirm(Actor actor, Long id) {
        return asRequester(actor, id, TicketEventType.CONFIRMED, Ticket::confirm);
    }

    @Transactional
    public TicketResponse reopen(Actor actor, Long id) {
        return asRequester(actor, id, TicketEventType.REOPENED, Ticket::reopen);
    }

    @Transactional
    public TicketResponse cancel(Actor actor, Long id) {
        return asRequester(actor, id, TicketEventType.CANCELLED, Ticket::cancel);
    }

    Ticket findVisible(Actor actor, Long id) {
        Ticket ticket = tickets.findById(id).orElseThrow(() -> ticketNotFound(id));
        if (!canSee(actor, ticket)) {
            throw ticketNotFound(id);
        }
        return ticket;
    }

    private TicketResponse asAssignee(Actor actor, Long id, TicketEventType type, Consumer<Ticket> action) {
        return asAssignee(actor, id, type, action, ticket -> null);
    }

    private TicketResponse asAssignee(Actor actor, Long id, TicketEventType type,
                                      Consumer<Ticket> action, Function<Ticket, String> detail) {
        Ticket ticket = findVisible(actor, id);
        if (!ticket.isAssignedTo(actor.user())) {
            throw new AccessDeniedException("Only the assigned agent can do this");
        }
        return applyAndRecord(actor, ticket, type, action, detail);
    }

    private TicketResponse asRequester(Actor actor, Long id, TicketEventType type, Consumer<Ticket> action) {
        Ticket ticket = findVisible(actor, id);
        if (!ticket.isRequestedBy(actor.user())) {
            throw new AccessDeniedException("Only the requester can do this");
        }
        return applyAndRecord(actor, ticket, type, action, t -> null);
    }

    private TicketResponse applyAndRecord(Actor actor, Ticket ticket, TicketEventType type,
                                          Consumer<Ticket> action, Function<Ticket, String> detail) {
        TicketStatus fromStatus = ticket.getStatus();
        action.accept(ticket);
        ticketHistory.record(ticket, userOf(actor), type, fromStatus, detail.apply(ticket));
        return saveAndRespond(ticket, actor);
    }

    private TicketResponse saveAndRespond(Ticket ticket, Actor actor) {
        tickets.flush();
        return toResponse(ticket, actor);
    }

    private AppUser userOf(Actor actor) {
        return users.getReferenceById(actor.user().getId());
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