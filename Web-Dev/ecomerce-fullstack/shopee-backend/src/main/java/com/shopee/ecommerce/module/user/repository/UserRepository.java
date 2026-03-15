package com.shopee.ecommerce.module.user.repository;

import com.shopee.ecommerce.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for {@link User} entities.
 *
 * <p>Follows the repository pattern — all DB queries live here,
 * never in services or controllers.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ── Lookup ────────────────────────────────────────────────

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    boolean existsByEmail(String email);

    // ── Admin queries ─────────────────────────────────────────

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email, Pageable pageable);

    // ── Status management ─────────────────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
    int updateActiveStatus(@Param("id") UUID id, @Param("active") boolean active);

    @Modifying
    @Query("UPDATE User u SET u.verified = true WHERE u.email = :email")
    int verifyEmail(@Param("email") String email);

    // ── Statistics ────────────────────────────────────────────
    Page<User> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String email, String fullName, Pageable pageable);

    long countByRole(User.Role role);
}
