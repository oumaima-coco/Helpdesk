package io.github.oumaimacoco.helpdesk.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.category.CategoryRepository;
import io.github.oumaimacoco.helpdesk.security.KeycloakRealmRoleConverter;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.team.TeamRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileAndCategoryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private CategoryRepository categories;

    private AppUser user;
    private Category category;

    @BeforeEach
    void createUserAndCategory() {
        user = users.save(new AppUser(UUID.randomUUID(), "user-" + UUID.randomUUID() + "@test.local", "Test User", "fr"));
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        category = categories.save(new Category("Imprimantes test", "طابعات اختبار", "Test printers", team));
    }

    @Test
    void userCanChangeTheirLanguage() throws Exception {
        mockMvc.perform(changeLanguage("ar"))
                .andExpect(status().isNoContent());

        assertThat(users.findById(user.getId()).orElseThrow().getPreferredLanguage()).isEqualTo("ar");
    }

    @Test
    void unsupportedLanguageIsRejected() throws Exception {
        mockMvc.perform(changeLanguage("de"))
                .andExpect(status().isBadRequest());

        assertThat(users.findById(user.getId()).orElseThrow().getPreferredLanguage()).isEqualTo("fr");
    }

    @Test
    void categoriesAreNamedInTheUserLanguage() throws Exception {
        mockMvc.perform(get("/api/categories").with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + category.getId() + ")].name").value("Imprimantes test"));

        mockMvc.perform(changeLanguage("ar")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories").with(asUser()))
                .andExpect(jsonPath("$[?(@.id == " + category.getId() + ")].name").value("طابعات اختبار"));
    }

    @Test
    void categoriesRequireLogin() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder changeLanguage(String language) {
        return put("/api/me/language")
                .with(asUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"language\": \"" + language + "\"}");
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asUser() {
        return jwt()
                .jwt(token -> token
                        .subject(user.getKeycloakId().toString())
                        .claim("email", user.getEmail())
                        .claim("name", user.getFullName())
                        .claim("realm_access", Map.of("roles", List.of("REQUESTER"))))
                .authorities(new KeycloakRealmRoleConverter());
    }
}