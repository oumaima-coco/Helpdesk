package io.github.oumaimacoco.helpdesk.ticket;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketAttachmentTest {

    private static final byte[] PNG = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 0x0D, 'I', 'H', 'D', 'R', 0, 0, 0, 1, 0, 0, 0, 1, 8, 2, 0, 0, 0};
    private static final byte[] HTML = "<html><body><script>alert('hacked')</script></body></html>"
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] EXECUTABLE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF, (byte) 0xFF};

    @TempDir
    static Path storageDirectory;

    @DynamicPropertySource
    static void useTemporaryStorage(DynamicPropertyRegistry registry) {
        registry.add("helpdesk.attachments.directory", () -> storageDirectory.toString());
    }

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
    private AppUser outsider;
    private AppUser admin;
    private Category category;
    private Ticket ticket;

    @BeforeEach
    void createTicketAndPeople() {
        Team team = teams.save(new Team("Test team " + UUID.randomUUID()));
        Team otherTeam = teams.save(new Team("Other team " + UUID.randomUUID()));
        category = categories.save(new Category("Test FR", "Test AR", "Test EN", team));

        requester = person("requester", null);
        outsider = person("outsider", otherTeam);
        admin = person("admin", null);
        ticket = newTicket();
    }

    @Test
    void requesterCanUploadAScreenshot() throws Exception {
        mockMvc.perform(upload(ticket, "screenshot.png", PNG, requester, Role.REQUESTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("screenshot.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(PNG.length))
                .andExpect(jsonPath("$.storageKey").doesNotExist());
    }

    @Test
    void uploadedFileDownloadsWithTheSameContentAsAnAttachment() throws Exception {
        String response = mockMvc.perform(upload(ticket, "screenshot.png", PNG, requester, Role.REQUESTER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long attachmentId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get(attachmentsUrl(ticket) + "/" + attachmentId).with(as(requester, Role.REQUESTER)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void htmlDisguisedAsPdfIsRejected() throws Exception {
        mockMvc.perform(upload(ticket, "invoice.pdf", HTML, requester, Role.REQUESTER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executableDisguisedAsImageIsRejected() throws Exception {
        mockMvc.perform(upload(ticket, "photo.png", EXECUTABLE, requester, Role.REQUESTER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        mockMvc.perform(upload(ticket, "empty.png", new byte[0], requester, Role.REQUESTER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void folderNamesAreRemovedFromTheFilename() throws Exception {
        mockMvc.perform(upload(ticket, "..\\..\\evil.png", PNG, requester, Role.REQUESTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("evil.png"));
    }

    @Test
    void attachmentOfAnotherTicketCannotBeDownloadedThroughThisOne() throws Exception {
        Ticket otherTicket = newTicket();
        String response = mockMvc.perform(upload(otherTicket, "private.png", PNG, requester, Role.REQUESTER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long attachmentId = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get(attachmentsUrl(ticket) + "/" + attachmentId).with(as(requester, Role.REQUESTER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotUpload() throws Exception {
        mockMvc.perform(upload(ticket, "screenshot.png", PNG, admin, Role.ADMIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void agentOfAnotherTeamCannotListTheAttachments() throws Exception {
        mockMvc.perform(get(attachmentsUrl(ticket)).with(as(outsider, Role.AGENT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotUploadToAClosedTicket() throws Exception {
        ticket.cancel();

        mockMvc.perform(upload(ticket, "screenshot.png", PNG, requester, Role.REQUESTER))
                .andExpect(status().isConflict());
    }

    private MockMultipartHttpServletRequestBuilder upload(
            Ticket target, String filename, byte[] bytes, AppUser user, Role... roles) {
        return multipart(attachmentsUrl(target))
                .file(new MockMultipartFile("file", filename, "application/octet-stream", bytes))
                .with(as(user, roles));
    }

    private String attachmentsUrl(Ticket target) {
        return "/api/tickets/" + target.getId() + "/attachments";
    }

    private Ticket newTicket() {
        String reference = "T-" + UUID.randomUUID().toString().substring(0, 8);
        return tickets.save(new Ticket(reference, "Test ticket", "Test description", Priority.P3, category, requester));
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