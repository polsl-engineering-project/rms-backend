package com.polsl.engineering.project.rms.security.jwt;

import com.auth0.jwt.algorithms.Algorithm;
import com.polsl.engineering.project.rms.security.UserCredentialsProvider;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import com.polsl.engineering.project.rms.security.UserPrincipalAuthenticationToken;
import com.polsl.engineering.project.rms.security.auth.dto.TokenPair;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import com.auth0.jwt.JWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final int TOKEN_LENGTH = 64;
    private static final int MAX_TOKENS_PER_USER = 5;
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(30);

    private final JwtProperties jwtProperties;
    private final Algorithm algorithm;
    private final Clock clock;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom;
    private final MessageDigest messageDigest;
    private final UserCredentialsProvider credentialsProvider;


    public TokenPair createTokens(Authentication authentication, String username, String deviceInfo, String ipAddress) {
        var accessToken = createAccessToken(authentication);

        enforceTokenLimit(UUID.fromString(authentication.getName()));
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        String tokenFamily = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(UUID.fromString(authentication.getName()))
                .username(username)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_VALIDITY))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .deviceInfo(sanitize(deviceInfo))
                .ipAddress(sanitize(ipAddress))
                .tokenFamily(tokenFamily)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, rawToken);
    }

    private String createAccessToken(Authentication authentication) {
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


    @Transactional
    public TokenPair refreshTokens(String rawToken, String deviceInfo, String ipAddress) {
        String tokenHash = hashToken(rawToken);

        RefreshToken oldToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            revokeToken(oldToken);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        if (wasRecentlyUsed(oldToken)) {
            revokeTokenFamily(oldToken.getTokenFamily());
            throw new InvalidRefreshTokenException("Token reuse detected - all tokens revoked");
        }

        var userCredentials = credentialsProvider
                .getUserCredentials(oldToken.getUsername())
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        var principal = userCredentials.toUserPrincipal();
        var authentication = new UserPrincipalAuthenticationToken(principal);

        validateDeviceAndIp(oldToken, deviceInfo, ipAddress);

        revokeToken(oldToken);

        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .userId(oldToken.getUserId())
                .username(oldToken.getUsername())
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_VALIDITY))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .deviceInfo(sanitize(deviceInfo))
                .ipAddress(sanitize(ipAddress))
                .tokenFamily(oldToken.getTokenFamily())
                .revoked(false)
                .build();

        refreshTokenRepository.save(newToken);

        String newAccessToken = createAccessToken(authentication);

        return new TokenPair(newAccessToken, newRawToken);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(this::revokeToken);
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .ifPresent(this::revokeToken);
    }

    private void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeTokenFamily(String tokenFamily) {
        List<RefreshToken> familyTokens = refreshTokenRepository
                .findByTokenFamilyAndRevokedFalse(tokenFamily);
        familyTokens.forEach(this::revokeToken);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        byte[] hash = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private boolean wasRecentlyUsed(RefreshToken token) {
        Duration timeSinceLastUse = Duration.between(token.getLastUsedAt(), Instant.now());
        return timeSinceLastUse.toSeconds() < 5;
    }

    private void validateDeviceAndIp(RefreshToken token, String deviceInfo, String ipAddress) {
        if (!token.getDeviceInfo().equals(sanitize(deviceInfo))) {
            throw new RefreshTokenCorruptionException("Device info corruption");
        }

        if (!token.getIpAddress().equals(sanitize(ipAddress))) {
            throw new RefreshTokenCorruptionException("IP Address corruption");
        }
    }


    private void enforceTokenLimit(UUID userId) {
        List<RefreshToken> userTokens = refreshTokenRepository
                .findByUserIdAndRevokedFalseOrderByCreatedAtAsc(userId);

        if (userTokens.size() >= MAX_TOKENS_PER_USER) {
            int tokensToRemove = userTokens.size() - MAX_TOKENS_PER_USER + 1;
            userTokens.stream()
                    .limit(tokensToRemove)
                    .forEach(this::revokeToken);
        }
    }

    private String sanitize(String input) {
        return input != null ? input.substring(0, Math.min(input.length(), 255)) : "";
    }


    @Scheduled(cron = "0 0 2 * * ?") // Codziennie o 2:00
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(60));
        refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedAtBefore(
                Instant.now(), cutoff
        );
    }

}
