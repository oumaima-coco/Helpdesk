package io.github.oumaimacoco.helpdesk.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final CurrentUserService currentUsers;
    private final AppUserRepository users;

    public ProfileController(CurrentUserService currentUsers, AppUserRepository users) {
        this.currentUsers = currentUsers;
        this.users = users;
    }

    @PutMapping("/language")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void changeLanguage(JwtAuthenticationToken authentication,
                               @Valid @RequestBody ChangeLanguageRequest request) {
        AppUser user = users.getReferenceById(currentUsers.currentActor(authentication).user().getId());
        user.changeLanguage(request.language());
    }
}