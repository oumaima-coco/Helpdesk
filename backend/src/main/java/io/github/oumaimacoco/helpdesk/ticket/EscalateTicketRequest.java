package io.github.oumaimacoco.helpdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EscalateTicketRequest(@NotBlank @Size(max = 1000) String reason) {
}