package io.github.oumaimacoco.helpdesk.category;

import io.github.oumaimacoco.helpdesk.user.CurrentUserService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categories;
    private final CurrentUserService currentUsers;

    public CategoryController(CategoryRepository categories, CurrentUserService currentUsers) {
        this.categories = categories;
        this.currentUsers = currentUsers;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(JwtAuthenticationToken authentication) {
        String language = currentUsers.currentActor(authentication).user().getPreferredLanguage();
        return categories.findAll(Sort.by("id")).stream()
                .map(category -> CategoryResponse.from(category, language))
                .toList();
    }
}