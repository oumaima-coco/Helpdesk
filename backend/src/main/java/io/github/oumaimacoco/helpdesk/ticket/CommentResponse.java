package io.github.oumaimacoco.helpdesk.ticket;

import java.time.Instant;

public record CommentResponse(Long id, String authorName, String body, boolean internal, Instant createdAt) {

    static CommentResponse from(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor().getFullName(),
                comment.getBody(),
                comment.isInternal(),
                comment.getCreatedAt());
    }
}