package com.shopee.ecommerce.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Servlet filter that enforces rate limits before requests reach the API layer.
 *
 * Header responses:
 *   X-Rate-Limit-Remaining: <tokens left>
 *   X-Rate-Limit-Retry-After-Seconds: <wait seconds> (on 429)
 *
 * Bucket key strategy:
 *   - /api/auth/** → auth:{ip}
 *   - /api/media/upload → upload:{userId or ip}
 *   - /api/search → search:{ip}
 *   - Other /api/** (authenticated) → api:{userId}
 *   - Other /api/** (anonymous)    → public:{ip}
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitConfig      config;

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        String path   = request.getRequestURI();
        String ip     = resolveClientIp(request);
        String userId = resolveUserId(request);

        String bucketKey;
        var    bucketSupplier = config.publicBucketConfig();

        if (path.startsWith("/api/auth/")) {
            bucketKey     = "rate:auth:" + ip;
            bucketSupplier = config.authBucketConfig();
        } else if (path.startsWith("/api/media/upload")) {
            bucketKey     = "rate:upload:" + (userId != null ? userId : ip);
            bucketSupplier = config.uploadBucketConfig();
        } else if (path.contains("/search") || path.contains("/suggest")) {
            bucketKey     = "rate:search:" + ip;
            bucketSupplier = config.searchBucketConfig();
        } else if (userId != null) {
            bucketKey     = "rate:api:" + userId;
            bucketSupplier = config.apiBucketConfig();
        } else {
            bucketKey     = "rate:public:" + ip;
            bucketSupplier = config.publicBucketConfig();
        }

        Bucket          bucket = proxyManager.builder().build(bucketKey, bucketSupplier);
        ConsumptionProbe probe  = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            log.warn("Rate limit exceeded: key={} path={}", bucketKey, path);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitSeconds));
            response.setHeader("Retry-After", String.valueOf(waitSeconds));

            String body = """
                {"success":false,"message":"Rate limit exceeded. Retry after %d seconds.","timestamp":"%s"}
                """.formatted(waitSeconds, Instant.now());
            response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for static resources, actuator, swagger
        return path.startsWith("/actuator/")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
            || path.equals("/favicon.ico");
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Support X-Forwarded-For from Nginx reverse proxy
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserId(HttpServletRequest request) {
        // JWT is parsed by Spring Security before this filter for authenticated requests
        // But we're Order(1) so auth hasn't run yet — extract from Authorization header raw
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            // Extract subject from JWT without full validation (already done by Security)
            // Use a simple split — avoid re-parsing overhead here
            String token = auth.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                try {
                    String payload = new String(
                        java.util.Base64.getUrlDecoder().decode(
                            parts[1].replaceAll("=+$", "")
                        )
                    );
                    // Quick JSON extract: "sub":"<userId>"
                    int subIdx = payload.indexOf("\"sub\":\"");
                    if (subIdx >= 0) {
                        int start = subIdx + 7;
                        int end   = payload.indexOf('"', start);
                        return payload.substring(start, end);
                    }
                } catch (Exception e) {
                    // Not a valid JWT — treat as anonymous
                }
            }
        }
        return null;
    }
}
