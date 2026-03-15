package com.shopee.ecommerce.infrastructure.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Structured audit logging for business-critical events.
 *
 * Each event is logged as a structured JSON object (via logstash-logback-encoder)
 * with dedicated fields for easy querying in Kibana / Grafana Loki:
 *
 *   eventType  — ORDER_PLACED | PAYMENT_RECEIVED | USER_REGISTERED | ADMIN_ACTION | ...
 *   actorId    — userId performing the action
 *   resourceId — affected resource (orderId, productId, etc.)
 *   detail     — human-readable summary
 *   outcome    — SUCCESS | FAILURE
 *
 * Usage:
 *   auditLogger.log(AuditEvent.of("ORDER_PLACED").actor(userId).resource(orderId).build());
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private static final org.slf4j.Logger AUDIT =
        org.slf4j.LoggerFactory.getLogger("AUDIT");

    public void log(AuditEvent event) {
        String traceId = MDC.get("traceId");

        AUDIT.info("AUDIT_EVENT",
                net.logstash.logback.marker.Markers.appendEntries(java.util.Map.of(
                        "eventType",  event.eventType(),
                        "actorId",    nvl(event.actorId()),
                        "resourceId", nvl(event.resourceId()),
                        "detail",     nvl(event.detail()),
                        "outcome",    event.outcome().name(),
                        "timestamp",  event.timestamp().toString(),
                        "traceId",    nvl(traceId)
                ))
        );
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── Event record ──────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AuditEvent(
        String    eventType,
        String    actorId,
        String    resourceId,
        String    detail,
        Outcome   outcome,
        Instant   timestamp
    ) {
        public static Builder of(String eventType) { return new Builder(eventType); }

        public enum Outcome { SUCCESS, FAILURE }

        public static class Builder {
            private final String eventType;
            private String  actorId;
            private String  resourceId;
            private String  detail;
            private Outcome outcome = Outcome.SUCCESS;

            Builder(String eventType) { this.eventType = eventType; }

            public Builder actor(String actorId)       { this.actorId    = actorId;    return this; }
            public Builder resource(String resourceId) { this.resourceId = resourceId; return this; }
            public Builder resource(Object id)         { this.resourceId = String.valueOf(id); return this; }
            public Builder detail(String detail)       { this.detail     = detail;     return this; }
            public Builder failure()                   { this.outcome    = Outcome.FAILURE; return this; }

            public AuditEvent build() {
                return new AuditEvent(eventType, actorId, resourceId, detail, outcome, Instant.now());
            }
        }
    }
}
