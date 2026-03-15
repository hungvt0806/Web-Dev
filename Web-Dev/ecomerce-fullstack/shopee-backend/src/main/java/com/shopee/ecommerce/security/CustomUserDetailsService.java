package com.shopee.ecommerce.security;

import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Spring Security {@link UserDetailsService} implementation.
 *
 * <p>Loads user details by email (username) from the database.
 * Used by the authentication provider during login and by the JWT filter
 * to reconstruct the authentication from a token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their email address.
     * Called by Spring Security during form-based login and JWT validation.
     *
     * @param email the user's email address (used as the username)
     * @return UserPrincipal wrapping the User entity
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        return UserPrincipal.fromUser(user);
    }

    /**
     * Load a user by their UUID (for token-based lookups where we have the user ID).
     *
     * @param id the user's UUID
     * @return UserPrincipal wrapping the User entity
     * @throws UsernameNotFoundException if no user with that ID exists
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new UsernameNotFoundException("User not found with id: " + id);
                });

        return UserPrincipal.fromUser(user);
    }
}
