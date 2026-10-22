package io.github.oumaimacoco.helpdesk.user;

import jakarta.validation.constraints.NotBlank;

public record ChangeLanguageRequest(@NotBlank String language) {
}