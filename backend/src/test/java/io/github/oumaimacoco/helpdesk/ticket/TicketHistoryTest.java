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
class TicketHistoryTest {

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
    private AppUser lead;
    private AppUser outsider;
    private Category category;

    @BeforeEach
    void createPeopleAndTeams() {
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        Team otherTeam = teams.save(new Team("Other team " + UUID.randomUUID()));
        category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));

        requester = person("requester", null);
        agent = person("agent", team);
        lead = person("lead", team);
        outsider = person("outsider", otherTeam);
    }

    @Test
    void creatingATicketRecordsTheFirstEvent() throws Exception {
        long id = createTicket();

        mockMvc.perform(get(historyUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("CREATED"))
                .andExpect(jsonPath("$[0].toStatus").value("NEW"))
                .andExpect(jsonPath("$[0].actorName").value(requester.getFullName()));
    }

    @Test
    void everyStepOfTheLifecycleIsRecordedInOrder() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);
        action(id, "start", agent, Role.AGENT);
        resolve(id, "Replaced the toner");
        action(id, "confirm", requester, Role.REQUESTER);

        mockMvc.perform(get(historyUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].type").value("CREATED"))
                .andExpect(jsonPath("$[1].type").value("TAKEN"))
                .andExpect(jsonPath("$[2].type").value("STARTED"))
                .andExpect(jsonPath("$[3].type").value("RESOLVED"))
                .andExpect(jsonPath("$[4].type").value("CONFIRMED"))
                .andExpect(jsonPath("$[4].fromStatus").value("RESOLVED"))
                .andExpect(jsonPath("$[4].toStatus").value("CLOSED"));
    }

    @Test
    void assignmentRecordsWhoAssignedAndToWhom() throws Exception {
        long id = createTicket();
        mockMvc.perform(post("/api/tickets/" + id + "/assign")
                        .with(as(lead, Role.AGENT, Role.TEAM_LEAD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\": " + agent.getId() + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(historyUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$[1].type").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].actorName").value(lead.getFullName()))
                .andExpect(jsonPath("$[1].detail").value(agent.getFullName()));
    }

    @Test
    void escalationReasonIsVisibleToStaffButHiddenFromTheRequester() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);
        mockMvc.perform(post("/api/tickets/" + id + "/escalate")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Too many tickets this week\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(historyUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$[2].type").value("ESCALATED"))
                .andExpect(jsonPath("$[2].detail").value("Too many tickets this week"));

        mockMvc.perform(get(historyUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$[2].type").value("ESCALATED"))
                .andExpect(jsonPath("$[2].detail").doesNotExist());
    }

    @Test
    void requesterReplyIsRecordedAsInformationProvided() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);
        action(id, "start", agent, Role.AGENT);
        mockMvc.perform(post("/api/tickets/" + id + "/request-info")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Which printer is it?\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(historyUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$[3].type").value("INFO_REQUESTED"))
                .andExpect(jsonPath("$[3].detail").value("Which printer is it?"));

        mockMvc.perform(post("/api/tickets/" + id + "/comments")
                        .with(as(requester, Role.REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"It is the HP printer on floor 2\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get(historyUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[4].type").value("INFO_PROVIDED"))
                .andExpect(jsonPath("$[4].toStatus").value("IN_PROGRESS"));
    }

    @Test
    void agentOfAnotherTeamCannotSeeTheHistory() throws Exception {
        long id = createTicket();

        mockMvc.perform(get(historyUrl(id)).with(as(outsider, Role.AGENT)))
                .andExpect(status().isNotFound());
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

    private void action(long id, String action, AppUser user, Role... roles) throws Exception {
        mockMvc.perform(post("/api/tickets/" + id + "/" + action).with(as(user, roles)))
                .andExpect(status().isOk());
    }

    private void resolve(long id, String resolution) throws Exception {
        mockMvc.perform(post("/api/tickets/" + id + "/resolve")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\": \"" + resolution + "\"}"))
                .andExpect(status().isOk());
    }

    private String historyUrl(long id) {
        return "/api/tickets/" + id + "/history";
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