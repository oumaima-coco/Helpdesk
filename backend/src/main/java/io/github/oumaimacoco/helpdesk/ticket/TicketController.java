package io.github.oumaimacoco.helpdesk.ticket;

import io.github.oumaimacoco.helpdesk.user.Actor;
import io.github.oumaimacoco.helpdesk.user.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;
    private final CurrentUserService currentUsers;

    public TicketController(TicketService tickets, CurrentUserService currentUsers) {
        this.tickets = tickets;
        this.currentUsers = currentUsers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(JwtAuthenticationToken authentication,
                                 @Valid @RequestBody CreateTicketRequest request) {
        return tickets.create(actor(authentication), request);
    }

    @GetMapping
    public List<TicketResponse> listVisible(JwtAuthenticationToken authentication) {
        return tickets.listVisible(actor(authentication));
    }

    @GetMapping("/assigned-to-me")
    public List<TicketResponse> listAssignedToMe(JwtAuthenticationToken authentication) {
        return tickets.listAssignedTo(actor(authentication));
    }

    @GetMapping("/{id}")
    public TicketResponse get(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.get(actor(authentication), id);
    }

    @GetMapping("/{id}/history")
    public List<EventResponse> history(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.history(actor(authentication), id);
    }

    @PostMapping("/{id}/take")
    public TicketResponse take(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.take(actor(authentication), id);
    }

    @PostMapping("/{id}/assign")
    public TicketResponse assign(JwtAuthenticationToken authentication, @PathVariable Long id,
                                 @Valid @RequestBody AssignTicketRequest request) {
        return tickets.assign(actor(authentication), id, request.agentId());
    }

    @PostMapping("/{id}/start")
    public TicketResponse start(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.start(actor(authentication), id);
    }

    @PostMapping("/{id}/request-info")
    public TicketResponse requestInfo(JwtAuthenticationToken authentication, @PathVariable Long id,
                                      @Valid @RequestBody RequestInfoRequest request) {
        return tickets.requestInfo(actor(authentication), id, request.question());
    }

    @PostMapping("/{id}/provide-info")
    public TicketResponse provideInfo(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.provideInfo(actor(authentication), id);
    }

    @PostMapping("/{id}/resolve")
    public TicketResponse resolve(JwtAuthenticationToken authentication, @PathVariable Long id,
                                  @Valid @RequestBody ResolveTicketRequest request) {
        return tickets.resolve(actor(authentication), id, request.resolution());
    }

    @PostMapping("/{id}/escalate")
    public TicketResponse escalate(JwtAuthenticationToken authentication, @PathVariable Long id,
                                   @Valid @RequestBody EscalateTicketRequest request) {
        return tickets.escalate(actor(authentication), id, request.reason());
    }

    @PostMapping("/{id}/confirm")
    public TicketResponse confirm(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.confirm(actor(authentication), id);
    }

    @PostMapping("/{id}/reopen")
    public TicketResponse reopen(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.reopen(actor(authentication), id);
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.cancel(actor(authentication), id);
    }

    private Actor actor(JwtAuthenticationToken authentication) {
        return currentUsers.currentActor(authentication);
    }
}