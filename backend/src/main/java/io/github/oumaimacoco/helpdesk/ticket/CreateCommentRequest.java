package io.github.oumaimacoco.helpdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank @Size(max = 5000) String body,
        Boolean internal) {

    public CreateCommentRequest {
        internal = Boolean.TRUE.equals(internal);
    }
}