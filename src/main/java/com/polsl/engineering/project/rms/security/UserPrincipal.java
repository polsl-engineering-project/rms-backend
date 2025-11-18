package com.polsl.engineering.project.rms.security;

import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record UserPrincipal(UUID id, List<Role> roles) implements Principal {
    public UserPrincipal(UUID id, List<Role> roles) {
        this.id = id;
        this.roles = Collections.unmodifiableList(roles);
    }

    @Override
    public String getName() {
        return id.toString();
    }

    public enum Role implements GrantedAuthority {
        ADMIN,
        MANAGER,
        WAITER,
        COOK,
        DRIVER;

        public static Optional<Role> safeValueOf(String roleName) {
            try {
                return Optional.of(Role.valueOf(roleName.replace("ROLE_", "")));
            } catch (IllegalArgumentException _) {
                return Optional.empty();
            }
        }

        @Override
        public String getAuthority() {
            return "ROLE_" + name();
        }
    }

}
