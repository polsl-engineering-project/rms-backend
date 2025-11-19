package com.polsl.engineering.project.rms.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.polsl.engineering.project.rms.security.UserCredentialsProvider;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import com.polsl.engineering.project.rms.security.auth.dto.TokenPair;
import com.polsl.engineering.project.rms.security.exception.JwtSubjectDoesNotExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String SECRET = "test-secret-1234567890";
    private static final long EXPIRATION_MILLIS = 3_600_000L;

    Clock clock;
    Algorithm algorithm;
    JwtService underTest;
    MessageDigest messageDigest;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserCredentialsProvider credentialsProvider;
    @Mock
    private JwtSubjectExistenceByIdVerifier subjectExistenceByIdVerifier;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        algorithm = Algorithm.HMAC256(SECRET);
        messageDigest = MessageDigest.getInstance("SHA-256");
        underTest = new JwtService(
                new JwtProperties(SECRET, EXPIRATION_MILLIS),
                new RefreshTokenProperties(64,5, 30),
                algorithm,
                clock,
                refreshTokenRepository,
                new SecureRandom(),
                messageDigest,
                credentialsProvider,
                subjectExistenceByIdVerifier
        );
    }

    @Test
    @DisplayName("Given authentication with roles When createJwt Then token contains subject roles and valid timestamps")
    void GivenAuthenticationWithRoles_WhenCreateTokens_ThenTokenContainsSubjectRolesAndValidTimestamps() {
        // given
        UUID userId = UUID.randomUUID();
        List<GrantedAuthority> authorities = List.of(
                authority("ADMIN"),
                authority("COOK")
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);

        // when
        TokenPair tokens = underTest.createTokens(authentication, authentication.getName(), "device", "ip");

        // then
        var decoded = JWT.decode(tokens.accessToken());
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("roles").asList(String.class))
                .containsExactly("ADMIN", "COOK");
        assertThat(decoded.getIssuedAtAsInstant())
                .isEqualTo(Instant.now(clock));
        assertThat(decoded.getExpiresAtAsInstant())
                .isEqualTo(Instant.now(clock).plusMillis(EXPIRATION_MILLIS));

        assertThat(decoded.getAlgorithm()).isEqualTo("HS256");
        assertThat(tokens.refreshToken()).isNotNull();
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

        when(subjectExistenceByIdVerifier.doesExist(userId.toString())).thenReturn(true);

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

        when(subjectExistenceByIdVerifier.doesExist(userId.toString())).thenReturn(true);

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

        when(subjectExistenceByIdVerifier.doesExist("not-a-uuid")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> underTest.parseJwt(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given recently used refresh token When JwtService refreshTokens Then revokes family and throws")
    void GivenRecentlyUsedToken_WhenRefreshTokens_ThenRevokesFamilyAndThrows() {
        // given
        String oldRawToken = "old-token";
        RefreshToken oldToken = RefreshToken.builder()
                .tokenHash("hash")
                .userId(UUID.randomUUID())
                .username("john.doe")
                .deviceInfo("device")
                .ipAddress("ip")
                .tokenFamily(UUID.randomUUID().toString())
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .lastUsedAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.findByTokenFamilyAndRevokedFalse(oldToken.getTokenFamily()))
                .thenReturn(List.of(oldToken));

        // when + then
        assertThatThrownBy(() -> underTest.refreshTokens(oldRawToken, "device", "ip"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Token reuse detected");

        verify(refreshTokenRepository).findByTokenFamilyAndRevokedFalse(oldToken.getTokenFamily());
    }

    @Test
    @DisplayName("Given token subject does not exist When parseJwt Then throws JwtSubjectDoesNotExistException")
    void GivenTokenSubjectDoesNotExist_WhenParseJwt_ThenThrowsJwtSubjectDoesNotExistException() {
        // given
        UUID userId = UUID.randomUUID();
        String token = JWT.create()
                .withSubject(userId.toString())
                .withClaim("roles", List.of("ADMIN"))
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm);

        when(subjectExistenceByIdVerifier.doesExist(userId.toString())).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> underTest.parseJwt(token))
                .isInstanceOf(JwtSubjectDoesNotExistException.class)
                .hasMessageContaining(userId.toString());
    }

    // helpers
    private static GrantedAuthority authority(String value) {
        return () -> value;
    }
}