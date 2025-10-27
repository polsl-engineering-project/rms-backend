package com.polsl.engineering.project.rms.security;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record UserCredentials(UUID id, String hashedPassword, List<UserPrincipal.Role> roles) {
    public UserCredentials(UUID id, String hashedPassword, List<UserPrincipal.Role> roles) {
        this.id = id;
        this.hashedPassword = hashedPassword;
        this.roles = Collections.unmodifiableList(roles);
    }

    public UserPrincipal toUserPrincipal(){
        return new UserPrincipal(id, roles);
    }
}
