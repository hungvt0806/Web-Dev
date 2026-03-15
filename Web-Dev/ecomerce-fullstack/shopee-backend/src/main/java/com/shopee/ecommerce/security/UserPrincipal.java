package com.shopee.ecommerce.security;

import com.shopee.ecommerce.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal wrapping our {@link User} entity.
 *
 * <p>This is the object stored in the SecurityContext after successful authentication.
 * It decouples Spring Security from the JPA entity layer — the entity is loaded once,
 * converted to this immutable value object, and used throughout the request lifecycle.
 */
@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails, org.springframework.security.oauth2.core.user.OAuth2User {

    private final UUID id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isActive;

    /**
     * Factory method — converts a {@link User} JPA entity into a {@link UserPrincipal}.
     *
     * @param user the persisted User entity
     * @return immutable UserPrincipal for use in the SecurityContext
     */
    public static UserPrincipal fromUser(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .fullName(user.getFullName())
                .authorities(authorities)
                .isActive(user.isActive())
                .build();
    }

    // ── UserDetails contract ──────────────────────────────────

    @Override
    public String getUsername() {
        return email;  // Email is the unique username
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    private Map<String, Object> attributes;

    public static UserPrincipal fromUser(User user, Map<String, Object> attributes) {
        UserPrincipal principal = UserPrincipal.fromUser(user);
        principal.attributes = attributes;
        return principal;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : Map.of();
    }

    @Override
    public String getName() {
        return email;
    }
}
