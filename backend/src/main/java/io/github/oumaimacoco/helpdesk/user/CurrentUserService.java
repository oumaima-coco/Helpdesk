package io.github.oumaimacoco.helpdesk.user;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private static final String DEFAULT_LANGUAGE = "fr";
    private static final String ROLE_PREFIX = "ROLE_";

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

    @Transactional
    public Actor currentActor(JwtAuthenticationToken authentication) {
        AppUser user = currentUser(authentication.getToken());
        Set<Role> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .flatMap(name -> Arrays.stream(Role.values()).filter(role -> role.name().equals(name)))
                .collect(Collectors.toSet());
        return new Actor(user, roles);
    }
}