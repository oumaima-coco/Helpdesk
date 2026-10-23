package io.github.oumaimacoco.helpdesk.ticket;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.category.CategoryRepository;
import io.github.oumaimacoco.helpdesk.security.KeycloakRealmRoleConverter;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.team.TeamRepository;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import io.github.oumaimacoco.helpdesk.user.AppUserRepository;
import io.github.oumaimacoco.helpdesk.user.Role;
import java.util.Arrays;
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
class TicketDeadlineFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private AppUserRepository users;

    private AppUser requester;
    private AppUser agent;
    private Category category;

    @BeforeEach
    void createPeopleAndTeams() {
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));
        requester = person("requester", null);
        agent = person("agent", team);
    }

    @Test
    void clockStartsOnTakePausesWhileWaitingAndResumesWhenTheRequesterReplies() throws Exception {
        long id = createTicket();

        mockMvc.perform(get(ticketUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.pickupDueAt").exists())
                .andExpect(jsonPath("$.resolutionDueAt").doesNotExist());

        mockMvc.perform(post(ticketUrl(id) + "/take").with(as(agent, Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupDueAt").doesNotExist())
                .andExpect(jsonPath("$.resolutionDueAt").exists());

        mockMvc.perform(post(ticketUrl(id) + "/start").with(as(agent, Role.AGENT)))
                .andExpect(status().isOk());

        mockMvc.perform(post(ticketUrl(id) + "/request-info")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Which printer is it?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutionDueAt").doesNotExist())
                .andExpect(jsonPath("$.pausedRemainingSeconds").isNumber());

        mockMvc.perform(post(ticketUrl(id) + "/comments")
                        .with(as(requester, Role.REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"The one on floor 2\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get(ticketUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resolutionDueAt").exists())
                .andExpect(jsonPath("$.pausedRemainingSeconds").doesNotExist());
    }

    @Test
    void escalationSendsTheTicketBackWithAFreshPickupDeadline() throws Exception {
        long id = createTicket();
        mockMvc.perform(post(ticketUrl(id) + "/take").with(as(agent, Role.AGENT)))
                .andExpect(status().isOk());

        mockMvc.perform(post(ticketUrl(id) + "/escalate")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Needs the network team\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.pickupDueAt").exists())
                .andExpect(jsonPath("$.resolutionDueAt").doesNotExist());
    }

    private long createTicket() throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .with(as(requester, Role.REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Printer jam\", \"description\": \"It jams\", \"categoryId\": "
                                + category.getId() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String ticketUrl(long id) {
        return "/api/tickets/" + id;
    }

    private AppUser person(String name, Team team) {
        AppUser user = new AppUser(UUID.randomUUID(), name + "-" + UUID.randomUUID() + "@test.local", "Test " + name, "en");
        if (team != null) {
            user.joinTeam(team);
        }
        return users.save(user);
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor as(AppUser user, Role... roles) {
        return jwt()
                .jwt(token -> token
                        .subject(user.getKeycloakId().toString())
                        .claim("email", user.getEmail())
                        .claim("name", user.getFullName())
                        .claim("realm_access", Map.of("roles", Arrays.stream(roles).map(Role::name).toList())))
                .authorities(new KeycloakRealmRoleConverter());
    }
}