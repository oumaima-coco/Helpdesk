package io.github.oumaimacoco.helpdesk.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.oumaimacoco.helpdesk.user.AppUser;
import io.github.oumaimacoco.helpdesk.user.CurrentUserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void returnDemoUser() {
        AppUser demoUser = new AppUser(UUID.randomUUID(), "demo@example.com", "Demo User", "fr");
        given(currentUserService.currentUser(any())).willReturn(demoUser);
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"REQUESTER", "AGENT", "TEAM_LEAD", "ADMIN"})
    void eachRoleIsReadFromTheKeycloakToken(String role) throws Exception {
        mockMvc.perform(get("/api/me").with(keycloakToken("demo-user", role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo-user"))
                .andExpect(jsonPath("$.roles[0]").value(role));
    }

    @Test
    void userWithSeveralRolesGetsAllOfThem() throws Exception {
        mockMvc.perform(get("/api/me").with(keycloakToken("lead", "AGENT", "TEAM_LEAD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(2))
                .andExpect(jsonPath("$.roles[0]").value("AGENT"))
                .andExpect(jsonPath("$.roles[1]").value("TEAM_LEAD"));
    }

    @Test
    void tokenWithoutRolesGivesNoRoles() throws Exception {
        mockMvc.perform(get("/api/me").with(jwt().jwt(token -> token.subject("nobody"))
                        .authorities(new KeycloakRealmRoleConverter())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(0));
    }

    @Test
    void responseIncludesTheUserProfile() throws Exception {
        mockMvc.perform(get("/api/me").with(keycloakToken("demo-user", "REQUESTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Demo User"))
                .andExpect(jsonPath("$.email").value("demo@example.com"));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor keycloakToken(
            String username, String... roles) {
        return jwt()
                .jwt(token -> token
                        .subject(username)
                        .claim("realm_access", Map.of("roles", List.of(roles))))
                .authorities(new KeycloakRealmRoleConverter());
    }
}