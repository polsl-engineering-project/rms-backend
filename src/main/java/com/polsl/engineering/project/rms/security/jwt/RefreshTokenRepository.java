package com.polsl.engineering.project.rms.security.jwt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    List<RefreshToken> findByUserIdAndRevokedFalseOrderByCreatedAtAsc(UUID userId);

    List<RefreshToken> findByTokenFamilyAndRevokedFalse(String tokenFamily);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < ?1 OR (t.revoked = true AND t.revokedAt < ?2)")
    int deleteByExpiresAtBeforeOrRevokedAtBefore(Instant expiresAt, Instant revokedAt);
}