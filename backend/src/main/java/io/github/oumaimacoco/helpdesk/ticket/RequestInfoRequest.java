package io.github.oumaimacoco.helpdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestInfoRequest(@NotBlank @Size(max = 5000) String question) {
}