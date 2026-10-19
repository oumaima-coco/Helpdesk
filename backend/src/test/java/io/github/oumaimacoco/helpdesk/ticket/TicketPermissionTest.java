package io.github.oumaimacoco.helpdesk.ticket;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class TicketPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private TicketRepository tickets;

    private AppUser requester;
    private AppUser otherRequester;
    private AppUser agent;
    private AppUser teammate;
    private AppUser outsideAgent;
    private AppUser lead;
    private AppUser admin;
    private Category category;

    @BeforeEach
    void createPeopleAndTeams() {
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        Team otherTeam = teams.save(new Team("Other team " + UUID.randomUUID()));
        category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));

        requester = person("requester", null);
        otherRequester = person("other-requester", null);
        agent = person("agent", team);
        teammate = person("teammate", team);
        outsideAgent = person("outside-agent", otherTeam);
        lead = person("lead", team);
        admin = person("admin", null);
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requesterSeesTheirOwnTicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(get("/api/tickets/" + ticket.getId()).with(as(requester, Role.REQUESTER)))
                .andExpect(status().isOk());
    }

    @Test
    void anotherRequesterCannotEvenSeeTheTicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(get("/api/tickets/" + ticket.getId()).with(as(otherRequester, Role.REQUESTER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentOfTheTeamSeesTheTicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(get("/api/tickets/" + ticket.getId()).with(as(agent, Role.AGENT)))
                .andExpect(status().isOk());
    }

    @Test
    void agentOfAnotherTeamCannotSeeTheTicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(get("/api/tickets/" + ticket.getId()).with(as(outsideAgent, Role.AGENT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminSeesTheTicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(get("/api/tickets/" + ticket.getId()).with(as(admin, Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void agentOfTheTeamCanTakeANewTicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/take").with(as(agent, Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void requesterCannotTakeATicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/take").with(as(requester, Role.REQUESTER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotTakeATicket() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/take").with(as(admin, Role.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCannotTakeATicketAlreadyAssignedToATeammate() throws Exception {
        Ticket ticket = newTicket();
        ticket.assignTo(teammate);
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/take").with(as(agent, Role.AGENT)))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheAssignedAgentCanStartWork() throws Exception {
        Ticket ticket = newTicket();
        ticket.assignTo(agent);
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/start").with(as(teammate, Role.AGENT)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/start").with(as(agent, Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void teamLeadCanAssignToAnAgentOfTheTeam() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/assign")
                        .with(as(lead, Role.AGENT, Role.TEAM_LEAD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\": " + agent.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeName").value(agent.getFullName()));
    }

    @Test
    void agentCannotAssignTickets() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/assign")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\": " + teammate.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamLeadCannotAssignToAnAgentOfAnotherTeam() throws Exception {
        Ticket ticket = newTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/assign")
                        .with(as(lead, Role.AGENT, Role.TEAM_LEAD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\": " + outsideAgent.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void resolvingRequiresAResolution() throws Exception {
        Ticket ticket = startedTicket();
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/resolve")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotResolveATicketThatWasNotStarted() throws Exception {
        Ticket ticket = newTicket();
        ticket.assignTo(agent);
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/resolve")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\": \"Fixed it\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheRequesterCanConfirmTheResolution() throws Exception {
        Ticket ticket = startedTicket();
        ticket.resolve("Replaced the toner");
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/confirm").with(as(agent, Role.AGENT)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/confirm").with(as(requester, Role.REQUESTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void adminCannotCreateTickets() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(as(admin, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Test\", \"description\": \"Test\", \"categoryId\": " + category.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    private AppUser person(String name, Team team) {
        AppUser user = new AppUser(UUID.randomUUID(), name + "-" + UUID.randomUUID() + "@test.local", "Test " + name, "en");
        if (team != null) {
            user.joinTeam(team);
        }
        return users.save(user);
    }

    private Ticket newTicket() {
        String reference = "T-" + UUID.randomUUID().toString().substring(0, 8);
        return tickets.save(new Ticket(reference, "Test ticket", "Test description", Priority.P3, category, requester));
    }

    private Ticket startedTicket() {
        Ticket ticket = newTicket();
        ticket.assignTo(agent);
        ticket.start();
        return ticket;
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