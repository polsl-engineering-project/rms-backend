package com.polsl.engineering.project.rms.security;

import java.util.Optional;

public interface UserPrincipalProvider {
     Optional<UserPrincipal> getUserPrincipal(AuthenticationCredentials hashedPassword);
}
