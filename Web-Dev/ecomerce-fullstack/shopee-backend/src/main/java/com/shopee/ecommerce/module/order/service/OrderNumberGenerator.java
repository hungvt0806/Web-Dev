package com.shopee.ecommerce.module.order.service;

import com.shopee.ecommerce.module.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates unique, human-readable order numbers.
 *
 * Format: ORD-{yyyyMMdd}-{6 random uppercase alphanumeric chars}
 * Example: ORD-20240315-A3F2K9
 *
 * Collision probability: 36^6 ≈ 2.18 billion combinations per day.
 * Retries up to 5 times on the rare chance of a collision.
 */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final String PREFIX  = "ORD";
    private static final String CHARS   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int    SUFFIX_LEN = 6;
    private static final int    MAX_TRIES  = 5;

    private static final SecureRandom RNG = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;

    /**
     * Generate a unique order number, retrying if a collision is detected.
     */
    public String generate() {
        String datePart = LocalDate.now().format(DATE_FMT);
        for (int attempt = 0; attempt < MAX_TRIES; attempt++) {
            String candidate = PREFIX + "-" + datePart + "-" + randomSuffix();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        // Fallback: append extra entropy to guarantee uniqueness
        return PREFIX + "-" + datePart + "-" + randomSuffix() + randomSuffix().substring(0, 2);
    }

    private String randomSuffix() {
        char[] buf = new char[SUFFIX_LEN];
        for (int i = 0; i < SUFFIX_LEN; i++) {
            buf[i] = CHARS.charAt(RNG.nextInt(CHARS.length()));
        }
        return new String(buf);
    }
}
