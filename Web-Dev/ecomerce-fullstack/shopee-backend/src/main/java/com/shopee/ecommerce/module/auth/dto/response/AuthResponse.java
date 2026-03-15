package com.shopee.ecommerce.module.auth.dto.response;

import com.shopee.ecommerce.module.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Returned after a successful login or register.
 * Contains both the access token and refresh token.
 */
@Getter
@Builder
public class AuthResponse {

    private final String tokenType = "Bearer";
    private final String accessToken;
    private final String refreshToken;
    private final long   expiresIn;     // Access token TTL in seconds

    // Embedded user info (avoids a second /me request on login)
    private final UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private UUID   id;
        private String email;
        private String fullName;
        private String avatarUrl;
        private User.Role role;
    }
}
