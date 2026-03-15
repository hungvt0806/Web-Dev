package com.shopee.ecommerce.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * AOP aspect that measures method execution time for Service layer methods.
 * Logs a WARN if execution exceeds 500 ms (potential N+1 or slow query).
 */
@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    private static final long SLOW_THRESHOLD_MS = 500;

    @Around("within(@org.springframework.stereotype.Service *)")
    public Object measureServiceMethods(ProceedingJoinPoint pjp) throws Throwable {
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            return pjp.proceed();
        } finally {
            sw.stop();
            long ms = sw.getTotalTimeMillis();
            if (ms > SLOW_THRESHOLD_MS) {
                log.warn("SLOW_SERVICE_METHOD method={}.{} durationMs={}",
                    pjp.getTarget().getClass().getSimpleName(),
                    pjp.getSignature().getName(),
                    ms
                );
            } else {
                log.debug("SERVICE method={}.{} durationMs={}",
                    pjp.getTarget().getClass().getSimpleName(),
                    pjp.getSignature().getName(),
                    ms
                );
            }
        }
    }
}
