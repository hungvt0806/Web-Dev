package com.shopee.ecommerce.config;

import com.shopee.ecommerce.security.CustomAccessDeniedHandler;
import com.shopee.ecommerce.security.JwtAuthenticationEntryPoint;
import com.shopee.ecommerce.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.shopee.ecommerce.security.oauth2.CustomOAuth2UserService;
import com.shopee.ecommerce.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.shopee.ecommerce.security.oauth2.OAuth2AuthenticationFailureHandler;
/**
 * Spring Security 6 Configuration.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Stateless session (JWT-based) — no HttpSession</li>
 *   <li>CSRF disabled — safe for stateless REST APIs with JWT</li>
 *   <li>Custom 401/403 JSON error responses</li>
 *   <li>Method-level security via @PreAuthorize/@Secured</li>
 *   <li>OAuth2 login for Google and Facebook</li>
 * </ul>
 *
 * <p>Public endpoints (no JWT required):
 * <ul>
 *   <li>POST /api/auth/** — register, login, refresh, password reset</li>
 *   <li>GET  /api/products/** — product browsing</li>
 *   <li>GET  /api/categories/** — category listing</li>
 *   <li>GET  /oauth2/** — OAuth2 flows</li>
 *   <li>GET  /swagger-ui/**, /api-docs/** — API documentation</li>
 *   <li>GET  /actuator/health — health check</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter     jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler   customAccessDeniedHandler;
    private final UserDetailsService          userDetailsService;
    private final CorsConfigurationSource     corsConfigurationSource;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    // ── URL Constants ─────────────────────────────────────────

    private static final String[] PUBLIC_POST_URLS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/payments/paypay/webhook",
            "/api/payments/stripe/webhook"
    };

    private static final String[] PUBLIC_GET_URLS = {
            "/api/products/**",
            "/api/categories/**",
            "/api/auth/verify-email",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/actuator/health"
    };

    // ── Security Filter Chain ─────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS ──────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // ── CSRF ──────────────────────────────────────────
            // Disabled: stateless JWT API doesn't use cookies for auth
            .csrf(AbstractHttpConfigurer::disable)

            // ── Session Management ────────────────────────────
            // STATELESS: no HttpSession created or used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )

            // ── Exception Handling ────────────────────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

            // ── Authorization Rules ───────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Allow preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public POST endpoints
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_URLS).permitAll()

                // Public GET endpoints
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()

                // OAuth2 endpoints
                .requestMatchers("/oauth2/**").permitAll()

                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )

            // ── OAuth2 Login ──────────────────────────────────
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.baseUri("/oauth2/authorize")
                        )
                        .redirectionEndpoint(endpoint ->
                                endpoint.baseUri("/oauth2/callback/*")
                        )
                        .userInfoEndpoint(endpoint ->
                                endpoint.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

            // ── JWT Filter ────────────────────────────────────
            // Insert before Spring's UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Authentication Beans ──────────────────────────────────

    /**
     * DAO-based authentication provider.
     * Uses our UserDetailsService + BCrypt password encoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expose the AuthenticationManager bean for use in AuthService.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder with default strength (10 rounds).
     * Strong enough for production; increase to 12 for higher security.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
