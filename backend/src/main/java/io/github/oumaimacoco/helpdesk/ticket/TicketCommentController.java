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
@RequestMapping("/api/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final TicketCommentService comments;
    private final CurrentUserService currentUsers;

    public TicketCommentController(TicketCommentService comments, CurrentUserService currentUsers) {
        this.comments = comments;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    public List<CommentResponse> list(JwtAuthenticationToken authentication, @PathVariable Long ticketId) {
        return comments.list(currentUsers.currentActor(authentication), ticketId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(JwtAuthenticationToken authentication, @PathVariable Long ticketId,
                               @Valid @RequestBody CreateCommentRequest request) {
        return comments.add(currentUsers.currentActor(authentication), ticketId, request);
    }
}