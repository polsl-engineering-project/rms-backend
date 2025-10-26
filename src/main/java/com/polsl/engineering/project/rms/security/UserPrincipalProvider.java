package com.polsl.engineering.project.rms.security;

import java.util.Optional;

public interface UserPrincipalProvider {
     UserPrincipal getUserPrincipal(UserCredentials userCredentials);
     Optional<UserCredentials> getUserCredentials(String username);
}
