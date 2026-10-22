package io.github.oumaimacoco.helpdesk.category;

public record CategoryResponse(Long id, String name) {

    static CategoryResponse from(Category category, String language) {
        return new CategoryResponse(category.getId(), category.nameIn(language));
    }
}