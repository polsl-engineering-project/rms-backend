package com.polsl.engineering.project.rms.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "test-secret-1234567890";
    private static final long EXPIRATION_MILLIS = 3_600_000L;

    Clock clock;
    Algorithm algorithm;

    JwtService underTest;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        underTest = new JwtService(new JwtProperties(SECRET, EXPIRATION_MILLIS), clock);
        algorithm = Algorithm.HMAC256(SECRET);
    }

    @Test
    @DisplayName("Given authentication with roles When createJwt Then token contains subject roles and valid timestamps")
    void GivenAuthenticationWithRoles_WhenCreateJwt_ThenTokenContainsSubjectRolesAndValidTimestamps() {
        // given
        UUID userId = UUID.randomUUID();
        List<GrantedAuthority> authorities = List.of(
                authority("ADMIN"),
                authority("COOK")
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);

        // when
        String token = underTest.createJwt(authentication);

        // then
        var decoded = JWT.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("roles").asList(String.class))
                .containsExactly("ADMIN", "COOK");
        assertThat(decoded.getIssuedAtAsInstant())
                .isEqualTo(Instant.now(clock));
        assertThat(decoded.getExpiresAtAsInstant())
                .isEqualTo(Instant.now(clock).plusMillis(EXPIRATION_MILLIS));

        assertThat(decoded.getAlgorithm()).isEqualTo("HS256");
    }

    @Test
    @DisplayName("Given valid signed token When parseJwt Then returns UserPrincipal with id and roles")
    void GivenValidSignedToken_WhenParseJwt_ThenReturnsUserPrincipalWithIdAndRoles() {
        // given
        UUID userId = UUID.randomUUID();
        String token = JWT.create()
                .withSubject(userId.toString())
                .withClaim("roles", List.of("ADMIN", "COOK"))
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        // when
        UserPrincipal principal = underTest.parseJwt(token);

        // then
        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.roles())
                .containsExactly(UserPrincipal.Role.ADMIN, UserPrincipal.Role.COOK);
    }

    @Test
    @DisplayName("Given token with unknown role When parseJwt Then ignores unknown roles and keeps valid ones")
    void GivenTokenWithUnknownRole_WhenParseJwt_ThenIgnoresUnknownRolesAndKeepsValidOnes() {
        // given
        UUID userId = UUID.randomUUID();
        String token = JWT.create()
                .withSubject(userId.toString())
                .withClaim("roles", List.of("ADMIN", "UNKNOWN"))
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        // when
        UserPrincipal principal = underTest.parseJwt(token);

        // then
        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.roles())
                .containsExactly(UserPrincipal.Role.ADMIN);
    }

    @Test
    @DisplayName("Given token with invalid subject When parseJwt Then throws IllegalArgumentException")
    void GivenTokenWithInvalidSubject_WhenParseJwt_ThenThrowsIllegalArgumentException() {
        // given
        String token = JWT.create()
                .withSubject("not-a-uuid")
                .withClaim("roles", List.of("ADMIN"))
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        // when / then
        assertThatThrownBy(() -> underTest.parseJwt(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // helpers
    private static GrantedAuthority authority(String value) {
        return () -> value;
    }
}