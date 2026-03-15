package com.shopee.ecommerce.module.auth.service;

import com.shopee.ecommerce.exception.BusinessException;
import com.shopee.ecommerce.exception.InvalidTokenException;
import com.shopee.ecommerce.exception.ResourceAlreadyExistsException;
import com.shopee.ecommerce.exception.ResourceNotFoundException;
import com.shopee.ecommerce.module.auth.dto.request.LoginRequest;
import com.shopee.ecommerce.module.auth.dto.request.RefreshTokenRequest;
import com.shopee.ecommerce.module.auth.dto.request.RegisterRequest;
import com.shopee.ecommerce.module.auth.dto.response.AuthResponse;
import com.shopee.ecommerce.module.auth.entity.RefreshToken;
import com.shopee.ecommerce.module.auth.repository.RefreshTokenRepository;
import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import com.shopee.ecommerce.security.JwtTokenProvider;
import com.shopee.ecommerce.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final int REFRESH_TOKEN_DAYS = 30;

    private final AuthenticationManager  authenticationManager;
    private final JwtTokenProvider       jwtTokenProvider;
    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.USER)
                .provider(User.AuthProvider.LOCAL)
                .active(true)
                .verified(false)
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        UserPrincipal principal = UserPrincipal.fromUser(user);
        return buildAuthResponse(principal, user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User logged in: {}", principal.getEmail());
        return buildAuthResponse(principal, user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new InvalidTokenException("Not a refresh token");
        }

        String jti = jwtTokenProvider.getJtiFromToken(token);
        RefreshToken stored = refreshTokenRepository.findByTokenId(jti)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        UserPrincipal principal = UserPrincipal.fromUser(user);
        return buildAuthResponse(principal, user);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        try {
            String jti = jwtTokenProvider.getJtiFromToken(request.getRefreshToken());
            refreshTokenRepository.findByTokenId(jti).ifPresent(t -> {
                t.setRevoked(true);
                refreshTokenRepository.save(t);
            });
        } catch (Exception e) {
            log.warn("Logout: could not revoke token — {}", e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(String email) {
        throw new BusinessException("Email service not configured");
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        throw new BusinessException("Email service not configured");
    }

    @Override
    public void verifyEmail(String token) {
        throw new BusinessException("Email service not configured");
    }

    // ── Private helpers ───────────────────────────────────────

    private AuthResponse buildAuthResponse(UserPrincipal principal, User user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal.getId());

        String jti = jwtTokenProvider.getJtiFromToken(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS);

        RefreshToken entity = RefreshToken.builder()
                .tokenId(jti)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        long expiresIn = jwtTokenProvider.getExpirationFromToken(accessToken).getTime()
                - System.currentTimeMillis();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole())
                        .build())
                .build();
    }
}