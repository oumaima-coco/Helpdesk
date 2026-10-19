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
class TicketCommentTest {

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

    @Autowired
    private TicketCommentRepository comments;

    private AppUser requester;
    private AppUser agent;
    private AppUser outsideAgent;
    private AppUser admin;
    private Ticket ticket;

    @BeforeEach
    void createTicketAndPeople() {
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        Team otherTeam = teams.save(new Team("Other team " + UUID.randomUUID()));
        Category category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));

        requester = person("requester", null);
        agent = person("agent", team);
        outsideAgent = person("outside-agent", otherTeam);
        admin = person("admin", null);

        String reference = "T-" + UUID.randomUUID().toString().substring(0, 8);
        ticket = tickets.save(new Ticket(reference, "Test ticket", "Test description", Priority.P3, category, requester));
        ticket.assignTo(agent);
        ticket.start();
    }

    @Test
    void requesterCanCommentOnTheirTicket() throws Exception {
        mockMvc.perform(comment(requester, "It still jams this morning", false, Role.REQUESTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value(requester.getFullName()))
                .andExpect(jsonPath("$.internal").value(false));
    }

    @Test
    void requesterCannotWriteInternalNotes() throws Exception {
        mockMvc.perform(comment(requester, "Secret", true, Role.REQUESTER))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentCanWriteInternalNotes() throws Exception {
        mockMvc.perform(comment(agent, "Check with the network team first", true, Role.AGENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internal").value(true));
    }

    @Test
    void requesterNeverSeesInternalNotes() throws Exception {
        comments.save(new TicketComment(ticket, agent, "Public answer", false));
        comments.save(new TicketComment(ticket, agent, "Internal note", true));

        mockMvc.perform(get(commentsUrl()).with(as(requester, Role.REQUESTER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].body").value("Public answer"));
    }

    @Test
    void agentSeesAllComments() throws Exception {
        comments.save(new TicketComment(ticket, agent, "Public answer", false));
        comments.save(new TicketComment(ticket, agent, "Internal note", true));

        mockMvc.perform(get(commentsUrl()).with(as(agent, Role.AGENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void adminCannotComment() throws Exception {
        mockMvc.perform(comment(admin, "Hello", false, Role.ADMIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentOfAnotherTeamCannotSeeTheComments() throws Exception {
        mockMvc.perform(get(commentsUrl()).with(as(outsideAgent, Role.AGENT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankCommentIsRejected() throws Exception {
        mockMvc.perform(comment(requester, "   ", false, Role.REQUESTER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requesterReplyMovesAWaitingTicketBackToInProgress() throws Exception {
        ticket.requestInfo();

        mockMvc.perform(comment(requester, "The printer model is HP 400", false, Role.REQUESTER))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/" + ticket.getId()).with(as(requester, Role.REQUESTER)))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void cannotCommentOnAClosedTicket() throws Exception {
        ticket.resolve("Replaced the roller");
        ticket.confirm();

        mockMvc.perform(comment(requester, "Thanks!", false, Role.REQUESTER))
                .andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder comment(
            AppUser author, String body, boolean internal, Role... roles) {
        return post(commentsUrl())
                .with(as(author, roles))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\": \"" + body + "\", \"internal\": " + internal + "}");
    }

    private String commentsUrl() {
        return "/api/tickets/" + ticket.getId() + "/comments";
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