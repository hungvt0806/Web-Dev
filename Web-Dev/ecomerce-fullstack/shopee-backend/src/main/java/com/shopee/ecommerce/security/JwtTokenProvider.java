package com.shopee.ecommerce.security;

import com.shopee.ecommerce.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT utility component.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate access tokens (short-lived, 15 min)</li>
 *   <li>Generate refresh tokens (long-lived, 30 days)</li>
 *   <li>Parse and validate tokens</li>
 *   <li>Extract claims (userId, email, roles)</li>
 * </ul>
 *
 * <p>Token structure (access token claims):
 * <pre>
 * {
 *   "sub":   "user-uuid",
 *   "email": "user@example.com",
 *   "roles": ["ROLE_USER"],
 *   "type":  "ACCESS",
 *   "iat":   ...,
 *   "exp":   ...
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    // ── Key ───────────────────────────────────────────────────

    /**
     * Decode the hex secret from config and build an HMAC-SHA256 key.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token Generation ──────────────────────────────────────

    /**
     * Generate a short-lived access token from an authenticated principal.
     *
     * @param authentication the Spring Security authentication object
     * @return signed JWT access token string
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }

    /**
     * Generate a short-lived access token from a UserPrincipal.
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + appProperties.getJwt().getAccessTokenExpiryMs());

        return Jwts.builder()
                .subject(userPrincipal.getId().toString())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate a long-lived refresh token.
     * The token is a random UUID-based JWT — its JTI is stored in the database
     * so it can be revoked individually.
     *
     * @param userId the user's UUID
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + appProperties.getJwt().getRefreshTokenExpiryMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .id(UUID.randomUUID().toString())   // jti — stored in DB for revocation
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ── Claims Extraction ─────────────────────────────────────

    /**
     * Parse all claims from a JWT string.
     * Throws JwtException subtypes on invalid/expired tokens.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the user ID (subject) from a token.
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    /**
     * Extract the email claim from an access token.
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /**
     * Extract the token type claim ("ACCESS" or "REFRESH").
     */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /**
     * Extract the JWT ID (jti) from a refresh token.
     * Used for DB-based revocation.
     */
    public String getJtiFromToken(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Get the token expiration date.
     */
    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    // ── Validation ────────────────────────────────────────────

    /**
     * Validate a JWT token.
     *
     * @param token the JWT string to validate
     * @return true if valid; false otherwise (also logs the reason)
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Validate that the token is specifically an ACCESS type token.
     */
    public boolean isAccessToken(String token) {
        return "ACCESS".equals(getTokenType(token));
    }

    /**
     * Validate that the token is specifically a REFRESH type token.
     */
    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(getTokenType(token));
    }
}
