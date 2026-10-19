package io.github.oumaimacoco.helpdesk.ticket;

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
        return tickets.create(currentUsers.currentActor(authentication), request);
    }

    @GetMapping
    public List<TicketResponse> listVisible(JwtAuthenticationToken authentication) {
        return tickets.listVisible(currentUsers.currentActor(authentication));
    }

    @GetMapping("/assigned-to-me")
    public List<TicketResponse> listAssignedToMe(JwtAuthenticationToken authentication) {
        return tickets.listAssignedTo(currentUsers.currentActor(authentication));
    }

    @GetMapping("/{id}")
    public TicketResponse get(JwtAuthenticationToken authentication, @PathVariable Long id) {
        return tickets.get(currentUsers.currentActor(authentication), id);
    }
}