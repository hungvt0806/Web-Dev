package com.shopee.ecommerce.infrastructure.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a unique traceId to every request and adds it to MDC.
 * The traceId is propagated in the X-Trace-Id response header.
 *
 * MDC fields available in every log line (used by Logback JSON encoder):
 *   traceId     — unique per request
 *   method      — HTTP method
 *   uri         — request path
 *   clientIp    — real client IP (after X-Forwarded-For)
 *   userId      — extracted from JWT if authenticated
 */
@Slf4j
@Component
@Order(0)   // Runs before rate-limit filter so all logs have traceId
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long   start   = System.currentTimeMillis();

        try {
            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put("method",   request.getMethod());
            MDC.put("uri",      request.getRequestURI());
            MDC.put("clientIp", resolveClientIp(request));

            response.setHeader(TRACE_HEADER, traceId);

            chain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - start;
            MDC.put("status",   String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(duration));

            // Log every request at INFO level (skip health checks)
            if (!request.getRequestURI().contains("/actuator/health")) {
                log.info("{} {} → {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration
                );
            }

            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
