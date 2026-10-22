package io.github.oumaimacoco.helpdesk.ticket;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
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
class TicketActionsTest {

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
    private Category category;

    @BeforeEach
    void createPeopleAndTeams() {
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));

        requester = person("requester", null);
        agent = person("agent", team);
        lead = person("lead", team);
    }

    @Test
    void newTicketOffersTakeToTeamAgentsAndCancelToTheRequester() throws Exception {
        long id = createTicket();

        mockMvc.perform(get(ticketUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions", contains("TAKE")));

        mockMvc.perform(get(ticketUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.actions", contains("CANCEL")));
    }

    @Test
    void assignedTicketOffersStartAndEscalateToItsAgentOnly() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);

        mockMvc.perform(get(ticketUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$.actions", contains("START", "ESCALATE")));

        mockMvc.perform(get(ticketUrl(id)).with(as(lead, Role.AGENT, Role.TEAM_LEAD)))
                .andExpect(jsonPath("$.actions", empty()));

        mockMvc.perform(get(ticketUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.actions", empty()));
    }

    @Test
    void ticketInProgressOffersRequestInfoResolveAndEscalate() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);
        action(id, "start", agent, Role.AGENT);

        mockMvc.perform(get(ticketUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$.actions", contains("REQUEST_INFO", "RESOLVE", "ESCALATE")));
    }

    @Test
    void resolvedTicketLetsTheRequesterConfirmOrReopen() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);
        action(id, "start", agent, Role.AGENT);
        resolve(id);

        mockMvc.perform(get(ticketUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.actions", contains("CONFIRM", "REOPEN")));

        mockMvc.perform(get(ticketUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$.actions", empty()));
    }

    @Test
    void closedTicketOffersNothing() throws Exception {
        long id = createTicket();
        action(id, "take", agent, Role.AGENT);
        action(id, "start", agent, Role.AGENT);
        resolve(id);
        action(id, "confirm", requester, Role.REQUESTER);

        mockMvc.perform(get(ticketUrl(id)).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.actions", empty()));

        mockMvc.perform(get(ticketUrl(id)).with(as(agent, Role.AGENT)))
                .andExpect(jsonPath("$.actions", empty()));
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

    private void resolve(long id) throws Exception {
        mockMvc.perform(post("/api/tickets/" + id + "/resolve")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\": \"Replaced the toner\"}"))
                .andExpect(status().isOk());
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