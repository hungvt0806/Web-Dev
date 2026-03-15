package com.shopee.ecommerce.module.auth.entity;

import com.shopee.ecommerce.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Persisted refresh token for DB-backed revocation.
 *
 * <p>When a user logs out or changes their password, all their refresh
 * tokens are revoked. The JTI (jti claim) from the JWT maps to {@code tokenId}.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_token_id", columnList = "token_id", unique = true),
        @Index(name = "idx_refresh_tokens_user_id",  columnList = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The JWT ID (jti) claim — uniquely identifies this refresh token.
     * Stored so we can revoke individual tokens.
     */
    @Column(name = "token_id", nullable = false, unique = true, length = 36)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Helpers ───────────────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
