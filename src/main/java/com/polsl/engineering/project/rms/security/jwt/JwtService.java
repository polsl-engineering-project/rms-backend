package com.polsl.engineering.project.rms.security.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import com.auth0.jwt.JWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Algorithm algorithm;
    private final Clock clock;

    public JwtService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.algorithm = Algorithm.HMAC256(jwtProperties.key());
    }

    public String createJwt(Authentication authentication) {
        return JWT.create()
                .withSubject(authentication.getName())
                .withClaim("roles", getRolesFromAuthentication(authentication))
                .withIssuedAt(Instant.now(clock))
                .withExpiresAt(Instant.now(clock).plusMillis(jwtProperties.expirationMillis()))
                .sign(algorithm);
    }

    public UserPrincipal parseJwt(String token) {
        var decodedJwt = JWT.require(algorithm)
                .build()
                .verify(token);

        var id = decodedJwt.getSubject();
        var roles = decodedJwt.getClaim("roles").asList(String.class);

        var authorities = roles.stream()
                .map(UserPrincipal.Role::safeValueOf)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return new UserPrincipal(UUID.fromString(id), authorities);
    }

    private List<String> getRolesFromAuthentication(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

}
