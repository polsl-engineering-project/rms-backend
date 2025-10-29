package com.polsl.engineering.project.rms.security.jwt;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "device_info", nullable = false)
    private String deviceInfo;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Version
    private Long version;

    @Column(name = "revoked_at", nullable = true)
    private Instant revokedAt;

    @Column(name = "token_family", nullable = false)
    private String tokenFamily;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var that = (RefreshToken) o;
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }
        return tokenHash != null && tokenHash.equals(that.tokenHash);
    }

    @Override
    public int hashCode() {
        return tokenHash != null ? tokenHash.hashCode() : 0;
    }

}
