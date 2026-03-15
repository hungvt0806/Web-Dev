package com.shopee.ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration — reads allowed origins from application.yml.
 * Referenced by SecurityConfig to wire into the security filter chain.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow configured origins (frontend + admin)
        config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());

        // Standard HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all headers including Authorization
        config.setAllowedHeaders(List.of("*"));

        // Expose Authorization header to frontend JS
        config.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));

        // Allow cookies / credentials
        config.setAllowCredentials(true);

        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
