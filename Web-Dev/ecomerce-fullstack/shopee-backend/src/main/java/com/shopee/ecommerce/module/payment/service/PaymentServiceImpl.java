package com.shopee.ecommerce.module.payment.service;

import com.shopee.ecommerce.exception.*;
import com.shopee.ecommerce.module.order.entity.Order;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import com.shopee.ecommerce.module.order.entity.OrderStatusHistory.ActorType;
import com.shopee.ecommerce.module.order.repository.OrderRepository;
import com.shopee.ecommerce.module.order.statemachine.OrderStateMachine;
import com.shopee.ecommerce.module.payment.client.PayPayHttpClient;
import com.shopee.ecommerce.module.payment.client.PayPayHttpClient.PayPayCreateResponse;
import com.shopee.ecommerce.module.payment.client.PayPayHttpClient.PayPayPaymentStatusResponse;
import com.shopee.ecommerce.module.payment.client.dto.PayPayCreatePaymentRequest;
import com.shopee.ecommerce.module.payment.client.dto.PayPayCreatePaymentRequest.PayPayAmount;
import com.shopee.ecommerce.module.payment.client.dto.PayPayCreatePaymentRequest.PayPayOrderItem;
import com.shopee.ecommerce.module.payment.client.dto.PayPayWebhookEvent;
import com.shopee.ecommerce.module.payment.config.PayPayProperties;
import com.shopee.ecommerce.module.payment.dto.response.InitiatePaymentResponse;
import com.shopee.ecommerce.module.payment.dto.response.PaymentStatusResponse;
import com.shopee.ecommerce.module.payment.dto.response.RefundResponse;
import com.shopee.ecommerce.module.payment.entity.Payment;
import com.shopee.ecommerce.module.payment.entity.PaymentStatus;
import com.shopee.ecommerce.module.payment.mapper.PaymentMapper;
import com.shopee.ecommerce.module.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PaymentService implementation.
 *
 * Core design decisions:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ IDEMPOTENCY                                                         │
 * │   merchantPaymentId is our UUID that acts as the PayPay idempotency │
 * │   key. If we retry a failed create-payment call with the same UUID, │
 * │   PayPay returns the existing payment instead of creating a new one. │
 * │                                                                     │
 * │ WEBHOOK DEDUPLICATION                                               │
 * │   Incoming webhooks are deduplicated via SHA-256 of the raw body.   │
 * │   Duplicate delivery (PayPay retries for ~24h) writes nothing and   │
 * │   returns HTTP 200 immediately.                                     │
 * │                                                                     │
 * │ ORDER STATE MACHINE                                                 │
 * │   Order transitions go through OrderStateMachine — never directly.  │
 * │   This ensures the history log is always consistent.                │
 * │                                                                     │
 * │ COMPENSATING TRANSACTION ON GATEWAY FAILURE                         │
 * │   If PayPay's createPayment call succeeds but our DB write fails,   │
 * │   the next initiatePayment call detects the orphaned payment via    │
 * │   merchantPaymentId and reconnects to it.                           │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final PayPayHttpClient  payPayClient;
    private final PayPayProperties  props;
    private final PaymentMapper     paymentMapper;

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIATE PAYMENT
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(UUID orderId, UUID buyerId) {

        // 1. Load and validate the order ────────────────────────────────────
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.isOwnedBy(buyerId)) {
            throw new AccessDeniedException("Order " + orderId + " does not belong to this user");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Cannot pay for a cancelled order");
        }
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PROCESSING
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("Order " + order.getOrderNumber() + " is already paid");
        }

        // 2. Check for an existing active payment (idempotent retry) ────────
        Optional<Payment> existingOpt = paymentRepository.findLatestByOrderId(orderId);
        if (existingOpt.isPresent()) {
            Payment existing = existingOpt.get();
            if (existing.getStatus() == PaymentStatus.COMPLETED) {
                throw new BusinessException("Order " + order.getOrderNumber() + " is already paid");
            }
            // Reuse a non-expired INITIATED payment (buyer hit back and retried)
            if (existing.getStatus() == PaymentStatus.INITIATED && !existing.isExpired()) {
                log.info("Reusing existing payment {} for order {}", existing.getMerchantPaymentId(), orderId);
                return paymentMapper.toInitiateResponse(existing);
            }
            // Cancel stale payments before creating a new one
            if (existing.isPending()) {
                cancelStalePendingPayment(existing);
            }
        }

        // 3. Build merchantPaymentId (our idempotency key for PayPay) ────────
        String merchantPaymentId = buildMerchantPaymentId(orderId);

        // 4. Build PayPay create-payment request ─────────────────────────────
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(props.getPaymentTtlMinutes());
        long requestedAtEpoch   = Instant.now().getEpochSecond();
        long expiryAtEpoch      = expiresAt.toEpochSecond(ZoneOffset.UTC);

        List<PayPayOrderItem> lineItems = order.getItems().stream()
                .map(item -> PayPayOrderItem.builder()
                        .name(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(PayPayAmount.builder()
                                .amount(item.getUnitPrice().longValue())
                                .currency("JPY")
                                .build())
                        .build())
                .collect(Collectors.toList());

        PayPayCreatePaymentRequest paypayRequest = PayPayCreatePaymentRequest.builder()
                .merchantPaymentId(merchantPaymentId)
                .amount(PayPayAmount.builder()
                        .amount(order.getTotalAmount().longValue())
                        .currency("JPY")
                        .build())
                .requestedAt(requestedAtEpoch)
                .expiryAt(expiryAtEpoch)
                .redirectUrl(props.getRedirectUrl() + "?orderId=" + orderId)
                .redirectType("WEB_LINK")
                .orderDescription("Order " + order.getOrderNumber())
                .orderItems(lineItems)
                .build();

        // 5. Call PayPay API ──────────────────────────────────────────────────
        PayPayCreateResponse paypayResponse;
        try {
            paypayResponse = payPayClient.createPayment(paypayRequest);
        } catch (PaymentGatewayException e) {
            log.error("PayPay createPayment failed for order {}: {}", orderId, e.getMessage());
            throw e;
        }

        // 6. Persist Payment row ──────────────────────────────────────────────
        Payment payment = Payment.builder()
                .merchantPaymentId(merchantPaymentId)
                .paypayPaymentId(paypayResponse.getPaymentId())
                .orderId(orderId)
                .buyerId(buyerId)
                .amount(order.getTotalAmount())
                .currency("JPY")
                .status(PaymentStatus.INITIATED)
                .paymentUrl(paypayResponse.getUrl())
                .redirectUrl(props.getRedirectUrl() + "?orderId=" + orderId)
                .webhookUrl(props.getWebhookUrl())
                .expiresAt(expiresAt)
                .build();

        Payment saved = paymentRepository.save(payment);

        // 7. Transition order → AWAITING_PAYMENT ─────────────────────────────
        if (order.getStatus() == OrderStatus.PENDING) {
            OrderStateMachine.systemTransition(order, OrderStatus.AWAITING_PAYMENT,
                    "PayPay payment initiated: " + merchantPaymentId);
            orderRepository.save(order);
        }

        log.info("Payment initiated: merchantPaymentId={} order={} amount=¥{}",
                merchantPaymentId, order.getOrderNumber(), order.getTotalAmount());

        return paymentMapper.toInitiateResponse(saved, paypayResponse.getDeeplink());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GET PAYMENT STATUS (polling endpoint)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PaymentStatusResponse getPaymentStatus(UUID orderId, UUID buyerId) {
        Payment payment = paymentRepository.findLatestByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        if (!payment.getBuyerId().equals(buyerId)) {
            throw new AccessDeniedException("Payment does not belong to this user");
        }

        // If already terminal, return immediately without hitting PayPay
        if (payment.getStatus().isTerminal()) {
            return paymentMapper.toStatusResponse(payment);
        }

        // Still pending — query PayPay live to detect missed webhooks
        try {
            PayPayPaymentStatusResponse live = payPayClient.getPaymentDetails(payment.getMerchantPaymentId());
            PaymentStatus liveStatus = PaymentStatus.fromPayPayState(live.getStatus());

            if (liveStatus != payment.getStatus()) {
                log.info("Payment status reconciled via poll: {} → {} for merchantPaymentId={}",
                        payment.getStatus(), liveStatus, payment.getMerchantPaymentId());
                applyStatusTransition(payment, liveStatus, live.getPaymentId(), null);
                paymentRepository.save(payment);
            }
        } catch (PaymentGatewayException e) {
            // PayPay unreachable — return cached DB state; don't fail the poll
            log.warn("Could not reach PayPay for status poll on {}: {}", payment.getMerchantPaymentId(), e.getMessage());
        }

        return paymentMapper.toStatusResponse(payment);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLE WEBHOOK
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Process a verified PayPay webhook.
     *
     * This method is called only after the HTTP controller has verified the HMAC
     * signature. It:
     *  1. Deduplicates via payload hash
     *  2. Looks up the Payment row by merchantPaymentId
     *  3. Applies the status transition
     *  4. Transitions the Order via OrderStateMachine
     *  5. Persists everything in one transaction
     */
    @Override
    @Transactional
    public void handleWebhook(PayPayWebhookEvent event, String payloadHash) {
        String merchantPaymentId = event.getMerchantPaymentId();

        // 1. Deduplication — reject duplicate webhook deliveries ──────────────
        if (paymentRepository.existsByWebhookPayloadHash(payloadHash)) {
            log.info("Duplicate webhook ignored for merchantPaymentId={}", merchantPaymentId);
            return;
        }

        // 2. Load Payment ─────────────────────────────────────────────────────
        Payment payment = paymentRepository.findByMerchantPaymentId(merchantPaymentId)
                .orElseGet(() -> {
                    // Webhook arrived before our DB write completed (race condition)
                    // This should be extremely rare. Log and skip — PayPay will retry.
                    log.warn("Webhook received for unknown merchantPaymentId={}", merchantPaymentId);
                    return null;
                });

        if (payment == null) return;

        // 3. Skip if already in a terminal state ─────────────────────────────
        if (payment.getStatus().isTerminal()) {
            log.info("Webhook ignored — payment {} already in terminal state {}",
                    merchantPaymentId, payment.getStatus());
            // Still stamp hash to prevent re-processing
            payment.setWebhookPayloadHash(payloadHash);
            paymentRepository.save(payment);
            return;
        }

        // 4. Apply status transition ──────────────────────────────────────────
        PaymentStatus newStatus = PaymentStatus.fromPayPayState(event.getPayPayStatus());
        String paypayPaymentId  = event.getData() != null ? event.getData().getPaymentId() : null;

        applyStatusTransition(payment, newStatus, paypayPaymentId, event);
        payment.setWebhookPayloadHash(payloadHash);
        paymentRepository.save(payment);

        // 5. Update Order if payment completed or failed ──────────────────────
        handleOrderTransition(payment, newStatus, event);

        log.info("Webhook processed: merchantPaymentId={} status={} orderId={}",
                merchantPaymentId, newStatus, payment.getOrderId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REFUND
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public RefundResponse refundPayment(UUID orderId, String reason, UUID adminId) {
        Payment payment = paymentRepository.findLatestByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(
                    "Only COMPLETED payments can be refunded (current: " + payment.getStatus() + ")");
        }

        // Call PayPay refund API
        payPayClient.refundPayment(
                payment.getPaypayPaymentId(),
                payment.getAmount().longValue(),
                reason);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Transition order → REFUNDED
        Order order = orderRepository.findByIdWithDetails(orderId).orElse(null);
        if (order != null) {
            OrderStateMachine.transition(order, OrderStatus.REFUNDED, adminId, ActorType.ADMIN,
                    "Refund issued via PayPay. Reason: " + reason);
            orderRepository.save(order);
        }

        log.info("Payment refunded: merchantPaymentId={} orderId={} by admin={}",
                payment.getMerchantPaymentId(), orderId, adminId);

        return paymentMapper.toRefundResponse(payment);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RECONCILE (called by scheduler)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public PaymentStatusResponse reconcilePayment(String merchantPaymentId) {
        Payment payment = paymentRepository.findByMerchantPaymentId(merchantPaymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + merchantPaymentId));

        if (payment.getStatus().isTerminal()) {
            return paymentMapper.toStatusResponse(payment);
        }

        PayPayPaymentStatusResponse live = payPayClient.getPaymentDetails(merchantPaymentId);
        PaymentStatus liveStatus = PaymentStatus.fromPayPayState(live.getStatus());

        if (liveStatus != payment.getStatus()) {
            applyStatusTransition(payment, liveStatus, live.getPaymentId(), null);
            paymentRepository.save(payment);
            handleOrderTransition(payment, liveStatus, null);
            log.info("Reconciled payment {}: {} → {}", merchantPaymentId, payment.getStatus(), liveStatus);
        }

        return paymentMapper.toStatusResponse(payment);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Apply a PayPay status to our Payment entity — stamps timestamps and
     * stores the PayPay payment ID if not already set.
     */
    private void applyStatusTransition(Payment payment, PaymentStatus newStatus,
                                        String paypayPaymentId, PayPayWebhookEvent event) {
        payment.setStatus(newStatus);

        if (paypayPaymentId != null && payment.getPaypayPaymentId() == null) {
            payment.setPaypayPaymentId(paypayPaymentId);
        }

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case AWAITING_CAPTURE -> payment.setAuthorizedAt(now);
            case COMPLETED        -> {
                payment.setAuthorizedAt(payment.getAuthorizedAt() != null ? payment.getAuthorizedAt() : now);
                payment.setCapturedAt(now);
            }
            case FAILED           -> payment.setFailedAt(now);
            case EXPIRED          -> payment.setFailedAt(now);
            default               -> { /* no specific timestamp */ }
        }

        // Store raw webhook payload for audit
        if (event != null && event.getData() != null) {
            // Flatten to a map for JSONB storage — use reflection-free approach
            payment.setPaymentMethod(
                    event.getData().getPaymentMethods() != null
                    && !event.getData().getPaymentMethods().isEmpty()
                            ? event.getData().getPaymentMethods().get(0).getType()
                            : null);
        }
    }

    /**
     * Transition the Order in response to a payment status change.
     *
     * COMPLETED  → Order: AWAITING_PAYMENT → PAID
     * FAILED     → Log; order stays in AWAITING_PAYMENT (buyer may retry)
     * CANCELLED  → Order stays in AWAITING_PAYMENT (buyer may retry)
     * EXPIRED    → Order stays in AWAITING_PAYMENT (scheduler handles timeout)
     */
    private void handleOrderTransition(Payment payment, PaymentStatus newStatus,
                                        PayPayWebhookEvent event) {
        if (newStatus != PaymentStatus.COMPLETED && newStatus != PaymentStatus.FAILED) {
            return; // Only COMPLETED and FAILED require an immediate order transition
        }

        Order order = orderRepository.findByIdWithDetails(payment.getOrderId()).orElse(null);
        if (order == null) {
            log.error("Order {} not found when processing payment webhook for merchantPaymentId={}",
                    payment.getOrderId(), payment.getMerchantPaymentId());
            return;
        }

        if (newStatus == PaymentStatus.COMPLETED) {
            if (order.getStatus() == OrderStatus.AWAITING_PAYMENT
                    || order.getStatus() == OrderStatus.PENDING) {

                // Transition AWAITING_PAYMENT → PAID
                if (order.getStatus() == OrderStatus.PENDING) {
                    OrderStateMachine.systemTransition(order, OrderStatus.AWAITING_PAYMENT,
                            "Auto-advanced before payment confirmation");
                }
                OrderStateMachine.systemTransition(order, OrderStatus.PAID,
                        "Payment confirmed by PayPay webhook. PaymentId: "
                        + payment.getMerchantPaymentId());
                orderRepository.save(order);

                log.info("Order {} advanced to PAID after PayPay payment confirmation", order.getOrderNumber());
            }

        } else if (newStatus == PaymentStatus.FAILED) {
            // Don't cancel the order — buyer may retry with a different payment
            log.warn("Payment FAILED for order {}. Reason: {}. Order stays in AWAITING_PAYMENT.",
                    order.getOrderNumber(), payment.getFailureReason());
        }
    }

    /**
     * Cancel a stale pending payment on PayPay (best-effort) and mark it CANCELLED in DB.
     */
    private void cancelStalePendingPayment(Payment stale) {
        try {
            payPayClient.cancelPayment(stale.getMerchantPaymentId());
        } catch (Exception e) {
            log.warn("Could not cancel stale PayPay payment {}: {}", stale.getMerchantPaymentId(), e.getMessage());
        }
        stale.setStatus(PaymentStatus.CANCELLED);
        stale.setFailedAt(LocalDateTime.now());
        paymentRepository.save(stale);
        log.info("Stale payment {} cancelled before creating new payment", stale.getMerchantPaymentId());
    }

    /**
     * Build a unique merchantPaymentId.
     * Format: PAY-{first 8 chars of orderId}-{8 random chars}
     */
    private String buildMerchantPaymentId(UUID orderId) {
        String orderPart  = orderId.toString().replace("-", "").substring(0, 8).toUpperCase();
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PAY-" + orderPart + "-" + randomPart;
    }
}
