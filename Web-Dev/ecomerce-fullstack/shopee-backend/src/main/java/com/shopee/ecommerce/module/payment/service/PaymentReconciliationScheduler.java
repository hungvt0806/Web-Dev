package com.shopee.ecommerce.module.payment.service;

import com.shopee.ecommerce.module.payment.entity.Payment;
import com.shopee.ecommerce.module.payment.entity.PaymentStatus;
import com.shopee.ecommerce.module.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled jobs for payment health and reconciliation.
 *
 * Two jobs:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ 1. RECONCILE (every 10 minutes)                                      │
 * │    Find INITIATED/AWAITING_CAPTURE payments with no webhook received │
 * │    in the last 15 minutes, then query PayPay directly to check their │
 * │    real status. Catches missed/delayed webhook deliveries.           │
 * │                                                                      │
 * │ 2. EXPIRE (every hour)                                               │
 * │    Bulk-mark payments whose expiresAt has passed as EXPIRED in DB.   │
 * │    These were never completed and the PayPay QR code has gone cold.  │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * In a multi-instance deployment, protect these with ShedLock:
 *   @SchedulerLock(name = "reconcilePayments", lockAtMostFor = "9m")
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentService    paymentService;

    /**
     * Reconcile payments that have been PENDING for more than 15 minutes
     * without a webhook. Runs every 10 minutes.
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void reconcileStuckPayments() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(15);
        List<Payment> stale = paymentRepository.findExpiredPendingPayments(
                LocalDateTime.now()); // finds those past their expiresAt

        if (stale.isEmpty()) return;

        log.info("Reconciliation job: {} payments to check", stale.size());
        int reconciled = 0;
        int errors     = 0;

        for (Payment payment : stale) {
            try {
                paymentService.reconcilePayment(payment.getMerchantPaymentId());
                reconciled++;
            } catch (Exception e) {
                errors++;
                log.warn("Reconciliation failed for {}: {}", payment.getMerchantPaymentId(), e.getMessage());
            }
        }

        log.info("Reconciliation complete: {} reconciled, {} errors", reconciled, errors);
    }

    /**
     * Bulk-expire payments past their TTL. Runs every hour.
     * This is a fast DB-only UPDATE — no PayPay API calls.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000L)
    @Transactional
    public void expireStalePayments() {
        int expired = paymentRepository.markExpiredBefore(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} stale payment(s)", expired);
        }
    }
}
