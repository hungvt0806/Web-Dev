package com.shopee.ecommerce.util;

import com.shopee.ecommerce.exception.ResourceNotFoundException;
import com.shopee.ecommerce.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility to access the currently authenticated user from the SecurityContext.
 *
 * <p>Usage in service layer:
 * <pre>
 * UUID currentUserId = SecurityUtils.getCurrentUserId();
 * </pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get the currently authenticated {@link UserPrincipal}.
     *
     * @return Optional containing the UserPrincipal, or empty if not authenticated
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Get the current user's UUID.
     *
     * @return the UUID of the authenticated user
     * @throws ResourceNotFoundException if no user is authenticated
     */
    public static UUID getCurrentUserId() {
        return getCurrentUser()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated user found"));
    }

    /**
     * Get the current user's email.
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser()
                .map(UserPrincipal::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated user found"));
    }

    /**
     * Check if the current user has a specific role.
     *
     * @param role role name WITHOUT the "ROLE_" prefix (e.g., "ADMIN", "USER")
     */
    public static boolean hasRole(String role) {
        return getCurrentUser()
                .map(user -> user.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)))
                .orElse(false);
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
