package com.polsl.engineering.project.rms.security;

import java.util.Optional;

public interface UserCredentialsProvider {
     Optional<UserCredentials> getUserCredentials(String username);
}
