package com.polsl.engineering.project.rms.security.jwt;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest extends ContainersEnvironment {

    @Autowired
    RefreshTokenRepository underTest;

    @Autowired
    TestEntityManager em;

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        em.flush();
    }

    @Test
    @DisplayName("Given expired refresh token, When deleting tokens before cutoff, Then expired token is removed")
    void GivenExpiredRefreshToken_WhenDeletingTokensBeforeCutoff_ThenExpiredTokenIsRemoved() {
        // Given
        UUID userId = UUID.randomUUID();

        RefreshToken expired = RefreshToken.builder()
                .userId(userId)
                .username("expired")
                .tokenHash("expiredHash")
                .deviceInfo("device")
                .ipAddress("ip")
                .expiresAt(Instant.now().minusSeconds(3600))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .revoked(false)
                .tokenFamily(UUID.randomUUID().toString())
                .build();

        RefreshToken valid = RefreshToken.builder()
                .userId(userId)
                .username("valid")
                .tokenHash("validHash")
                .deviceInfo("device")
                .ipAddress("ip")
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .revoked(false)
                .tokenFamily(UUID.randomUUID().toString())
                .build();

        // Persistujemy i flushujemy
        em.persist(expired);
        em.persist(valid);
        em.flush();

        // When
        underTest.deleteByExpiresAtBeforeOrRevokedAtBefore(Instant.now(), Instant.now().minusSeconds(1800));
        em.flush();

        // Then
        List<RefreshToken> remaining = underTest.findAll();
        assertThat(remaining).hasSize(1)
                .extracting(RefreshToken::getTokenHash)
                .containsExactly("validHash");
    }

    @Test
    @DisplayName("Given revoked token before cutoff, When deleting tokens, Then revoked token is removed")
    void GivenRevokedTokenBeforeCutoff_WhenDeletingTokens_ThenRevokedTokenIsRemoved() {
        // Given
        UUID userId = UUID.randomUUID();

        RefreshToken revoked = RefreshToken.builder()
                .userId(userId)
                .username("revoked")
                .tokenHash("revokedHash")
                .deviceInfo("device")
                .ipAddress("ip")
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .revoked(true)
                .revokedAt(Instant.now().minusSeconds(3600))
                .tokenFamily(UUID.randomUUID().toString())
                .build();

        RefreshToken valid = RefreshToken.builder()
                .userId(userId)
                .username("valid")
                .tokenHash("validHash")
                .deviceInfo("device")
                .ipAddress("ip")
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .revoked(false)
                .tokenFamily(UUID.randomUUID().toString())
                .build();

        em.persist(revoked);
        em.persist(valid);
        em.flush();

        // When
        underTest.deleteByExpiresAtBeforeOrRevokedAtBefore(Instant.now(), Instant.now().minusSeconds(1800));
        em.flush();

        // Then
        List<RefreshToken> remaining = underTest.findAll();
        assertThat(remaining).hasSize(1)
                .extracting(RefreshToken::getTokenHash)
                .containsExactly("validHash");
    }

    @Test
    @DisplayName("Given multiple tokens, When querying by token family, Then returns all non-revoked tokens of family")
    void GivenMultipleTokens_WhenQueryingByFamily_ThenReturnsNonRevokedTokens() {
        // Given
        String family = UUID.randomUUID().toString();

        RefreshToken token1 = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .username("user1")
                .tokenHash("hash1")
                .tokenFamily(family)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .deviceInfo("device")
                .ipAddress("ip")
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build();

        RefreshToken token2 = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .username("user2")
                .tokenHash("hash2")
                .tokenFamily(family)
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .deviceInfo("device")
                .ipAddress("ip")
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build();

        em.persist(token1);
        em.persist(token2);
        em.flush();

        // When
        List<RefreshToken> familyTokens = underTest.findByTokenFamilyAndRevokedFalse(family);

        // Then
        assertThat(familyTokens).hasSize(2)
                .extracting(RefreshToken::getTokenHash)
                .containsExactlyInAnyOrder("hash1", "hash2");
    }
}
