package com.shopee.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 *
 * <p>Executed once per request (extends {@link OncePerRequestFilter}).
 *
 * <p>Processing flow:
 * <ol>
 *   <li>Extract the Bearer token from the Authorization header</li>
 *   <li>Validate the token signature and expiry via {@link JwtTokenProvider}</li>
 *   <li>Load the user from DB via {@link UserDetailsService}</li>
 *   <li>Set the authentication in the {@link SecurityContextHolder}</li>
 * </ol>
 *
 * <p>If any step fails, the filter simply does NOT set authentication,
 * and Spring Security's access control will reject the request with 401.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";

    private final JwtTokenProvider    jwtTokenProvider;
    private final UserDetailsService  userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = extractBearerToken(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Only accept ACCESS tokens in the Authorization header
                if (!jwtTokenProvider.isAccessToken(jwt)) {
                    log.warn("Refresh token used as access token — rejected");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Load user by email (extracted from token claims)
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Build Spring Security authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials cleared after auth
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Store authentication in SecurityContext for this request
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user '{}' for URI: {}", email, request.getRequestURI());
            }
        } catch (Exception e) {
            // Log but do NOT propagate — let Spring Security handle the 401
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the raw JWT string from the Authorization: Bearer <token> header.
     *
     * @return the token string, or null if the header is missing/malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * Skip JWT processing for public endpoints that don't require authentication.
     * Spring Security's permitAll() handles access control, but we skip the filter
     * work entirely for efficiency on high-traffic public routes.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.equals("/actuator/health");
    }
}
