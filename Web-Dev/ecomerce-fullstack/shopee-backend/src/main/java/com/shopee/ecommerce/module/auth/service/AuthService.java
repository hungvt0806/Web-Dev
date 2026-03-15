package com.shopee.ecommerce.module.auth.service;

import com.shopee.ecommerce.module.auth.dto.request.LoginRequest;
import com.shopee.ecommerce.module.auth.dto.request.RefreshTokenRequest;
import com.shopee.ecommerce.module.auth.dto.request.RegisterRequest;
import com.shopee.ecommerce.module.auth.dto.response.AuthResponse;

/**
 * Authentication service contract.
 *
 * <p>Business logic for user registration, login, token lifecycle,
 * and password management. Implementation in {@link AuthServiceImpl}.
 */
public interface AuthService {

    /**
     * Register a new user with email/password.
     * Sends verification email after successful registration.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate a user with email/password credentials.
     * Returns access + refresh tokens on success.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Issue a new access token using a valid refresh token.
     * The old refresh token is rotated (revoked and replaced).
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Revoke the given refresh token (logout).
     */
    void logout(RefreshTokenRequest request);

    /**
     * Initiate the forgot-password flow — send reset email.
     * Silently succeeds even if the email doesn't exist (prevents enumeration).
     */
    void sendPasswordResetEmail(String email);

    /**
     * Complete the password reset using the emailed token.
     */
    void resetPassword(String token, String newPassword);

    /**
     * Mark the user's email as verified.
     */
    void verifyEmail(String token);
}
