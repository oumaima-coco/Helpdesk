package io.github.oumaimacoco.helpdesk.user;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserSummary> listUsers() {
        return userAdminService.listUsers();
    }

    @PutMapping("/{id}/team")
    public UserSummary assignTeam(@PathVariable Long id, @Valid @RequestBody AssignTeamRequest request) {
        return userAdminService.assignTeam(id, request.teamId());
    }
}