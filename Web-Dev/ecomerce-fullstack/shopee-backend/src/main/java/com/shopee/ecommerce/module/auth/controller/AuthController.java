package com.shopee.ecommerce.module.auth.controller;

import com.shopee.ecommerce.module.auth.dto.request.LoginRequest;
import com.shopee.ecommerce.module.auth.dto.request.RefreshTokenRequest;
import com.shopee.ecommerce.module.auth.dto.request.RegisterRequest;
import com.shopee.ecommerce.module.auth.dto.response.AuthResponse;
import com.shopee.ecommerce.module.auth.service.AuthService;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.exception.ResourceNotFoundException;
/**
 * Authentication REST Controller.
 *
 * <p>All endpoints under /api/auth/** are publicly accessible (no JWT required).
 * Configured as permitted in {@link com.shopee.ecommerce.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, Token refresh, Password reset")
public class AuthController {

    private final AuthService authService;

    private final UserRepository userRepository;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Get a new access token using a refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoke the current refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
        authService.sendPasswordResetEmail(email);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token from email")
    public ResponseEntity<Void> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {
        authService.resetPassword(token, newPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address from the verification link")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
