package com.polsl.engineering.project.rms.security;

import com.polsl.engineering.project.rms.security.exception.InvalidCredentialsException;
import com.polsl.engineering.project.rms.security.exception.NullCredentialsException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public record UserPrincipalAuthenticationProvider(
        PasswordEncoder passwordEncoder,
        UserCredentialsProvider credentialsProvider
) implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
            throw new IllegalArgumentException("Unsupported authentication type");
        }

        if (authentication.getCredentials() == null) {
            throw new NullCredentialsException();
        }

        var rawPassword = authentication.getCredentials().toString();
        var user = credentialsProvider.getUserCredentials(authentication.getName())
                .orElseThrow(InvalidCredentialsException::new);

        if(!passwordEncoder.matches(rawPassword, user.hashedPassword())){
            throw new InvalidCredentialsException();
        }

        return new UserPrincipalAuthenticationToken(user.toUserPrincipal());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
