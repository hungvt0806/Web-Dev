package com.shopee.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles unauthenticated access attempts.
 *
 * <p>Triggered by Spring Security when a request reaches a protected endpoint
 * without a valid JWT. Returns a clean JSON 401 response instead of
 * Spring's default HTML error page.
 *
 * <p>Example response:
 * <pre>
 * {
 *   "status":    401,
 *   "error":     "Unauthorized",
 *   "message":   "Full authentication is required to access this resource",
 *   "path":      "/api/orders",
 *   "timestamp": "2024-01-01T12:00:00"
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest      request,
            HttpServletResponse     response,
            AuthenticationException authException
    ) throws IOException {

        log.warn("Unauthorized request to '{}': {}", request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    401);
        body.put("error",     "Unauthorized");
        body.put("message",   authException.getMessage());
        body.put("path",      request.getServletPath());
        body.put("timestamp", LocalDateTime.now().toString());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
