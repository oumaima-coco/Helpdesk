package io.github.oumaimacoco.helpdesk.user;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private static final String DEFAULT_LANGUAGE = "fr";

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional
    public AppUser currentUser(Jwt token) {
        UUID keycloakId = UUID.fromString(token.getSubject());
        String email = token.getClaimAsString("email");
        String fullName = token.getClaimAsString("name");

        return users.findByKeycloakId(keycloakId)
                .map(user -> {
                    user.updateProfile(email, fullName);
                    return user;
                })
                .orElseGet(() -> users.save(new AppUser(keycloakId, email, fullName, DEFAULT_LANGUAGE)));
    }
}