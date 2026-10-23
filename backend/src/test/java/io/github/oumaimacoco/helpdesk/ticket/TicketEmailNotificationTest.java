package io.github.oumaimacoco.helpdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.oumaimacoco.helpdesk.category.Category;
import io.github.oumaimacoco.helpdesk.category.CategoryRepository;
import io.github.oumaimacoco.helpdesk.notification.OutboxEmailRepository;
import io.github.oumaimacoco.helpdesk.security.KeycloakRealmRoleConverter;
import io.github.oumaimacoco.helpdesk.team.Team;
import io.github.oumaimacoco.helpdesk.team.TeamRepository;
import io.github.oumaimacoco.helpdesk.user.AppUser;
import io.github.oumaimacoco.helpdesk.user.AppUserRepository;
import io.github.oumaimacoco.helpdesk.user.Role;
import java.time.Instant;
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
class TicketEmailNotificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxEmailRepository emails;

    @Autowired
    private OverdueTicketService overdueTickets;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private AppUserRepository users;

    private Team team;
    private AppUser requester;
    private AppUser agent;
    private AppUser lead;
    private Category category;

    @BeforeEach
    void createPeopleAndTeams() {
        team = teams.save(new Team("Test team " + UUID.randomUUID()));
        category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));
        requester = person("requester", null, "fr", false);
        agent = person("agent", team, "en", false);
        lead = person("lead", team, "en", true);
    }

    @Test
    void takingTheTicketEmailsTheRequesterInTheirLanguageWithoutTicketDetails() throws Exception {
        Created ticket = createTicket();

        action(ticket.id(), "take");

        assertThat(emails.findByRecipientOrderByIdAsc(requester.getEmail()))
                .singleElement()
                .satisfies(email -> {
                    assertThat(email.getSubject()).contains(ticket.reference());
                    assertThat(email.getBody())
                            .contains("Votre ticket est pris en charge")
                            .contains("/tickets/" + ticket.id())
                            .doesNotContain("Printer jam")
                            .doesNotContain("It jams");
                });
        assertThat(emails.findByRecipientOrderByIdAsc(agent.getEmail())).isEmpty();
    }

    @Test
    void requestingInformationEmailsTheRequesterAndTheReplyEmailsTheAgent() throws Exception {
        Created ticket = createTicket();
        action(ticket.id(), "take");
        action(ticket.id(), "start");
        mockMvc.perform(post("/api/tickets/" + ticket.id() + "/request-info")
                        .with(as(agent, Role.AGENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"Which printer is it?\"}"))
                .andExpect(status().isOk());

        assertThat(emails.findByRecipientOrderByIdAsc(requester.getEmail()))
                .hasSize(3)
                .last()
                .satisfies(email -> assertThat(email.getBody()).contains("Des informations vous sont demandées"));

        mockMvc.perform(post("/api/tickets/" + ticket.id() + "/comments")
                        .with(as(requester, Role.REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"The one on floor 2\"}"))
                .andExpect(status().isCreated());

        assertThat(emails.findByRecipientOrderByIdAsc(agent.getEmail()))
                .singleElement()
                .satisfies(email -> assertThat(email.getBody()).contains("The requester has replied"));
    }

    @Test
    void overdueTicketEmailsThePreviousAgentAndTheTeamLead() throws Exception {
        Created ticket = createTicket();
        action(ticket.id(), "take");
        Ticket entity = tickets.findById(ticket.id()).orElseThrow();
        entity.scheduleDeadlines(null, Instant.now().minusSeconds(60), null);
        tickets.flush();

        overdueTickets.returnOverdueTickets();

        assertThat(emails.findByRecipientOrderByIdAsc(agent.getEmail()))
                .singleElement()
                .satisfies(email -> assertThat(email.getBody()).contains("A ticket you had passed its deadline"));
        assertThat(emails.findByRecipientOrderByIdAsc(lead.getEmail()))
                .singleElement()
                .satisfies(email -> assertThat(email.getBody()).contains("A ticket passed its deadline"));
    }

    @Test
    void teamLeadFlagFollowsTheRolesInTheToken() throws Exception {
        AppUser colleague = person("colleague", team, "en", false);

        mockMvc.perform(get("/api/tickets").with(as(colleague, Role.AGENT, Role.TEAM_LEAD)))
                .andExpect(status().isOk());
        assertThat(users.findById(colleague.getId()).orElseThrow().isTeamLead()).isTrue();

        mockMvc.perform(get("/api/tickets").with(as(colleague, Role.AGENT)))
                .andExpect(status().isOk());
        assertThat(users.findById(colleague.getId()).orElseThrow().isTeamLead()).isFalse();
    }

    private Created createTicket() throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .with(as(requester, Role.REQUESTER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Printer jam\", \"description\": \"It jams\", \"categoryId\": "
                                + category.getId() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(response, "$.id")).longValue();
        String reference = JsonPath.read(response, "$.reference");
        return new Created(id, reference);
    }

    private void action(long id, String action) throws Exception {
        mockMvc.perform(post("/api/tickets/" + id + "/" + action).with(as(agent, Role.AGENT)))
                .andExpect(status().isOk());
    }

    private AppUser person(String name, Team personTeam, String language, boolean teamLead) {
        AppUser user = new AppUser(UUID.randomUUID(), name + "-" + UUID.randomUUID() + "@test.local", "Test " + name, language);
        if (personTeam != null) {
            user.joinTeam(personTeam);
        }
        user.syncTeamLead(teamLead);
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

    private record Created(long id, String reference) {
    }
}