package com.shopee.ecommerce.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User JPA entity — maps to the `users` table.
 *
 * <p>Supports both local email/password auth and OAuth2 social login.
 * For OAuth2 users, {@code passwordHash} is null.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email",       columnList = "email",       unique = true),
        @Index(name = "idx_users_provider_id", columnList = "provider_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String passwordHash;   // null for OAuth2 users

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 255)
    private String providerId;     // OAuth2 sub/id from provider

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────

    public enum Role {
        USER, ADMIN, SELLER
    }

    public enum AuthProvider {
        LOCAL, GOOGLE, FACEBOOK
    }
}
