package com.shopee.ecommerce.module.payment.mapper;

import com.shopee.ecommerce.module.payment.client.PayPayHttpClient.PayPayCreateResponse;
import com.shopee.ecommerce.module.payment.dto.response.InitiatePaymentResponse;
import com.shopee.ecommerce.module.payment.dto.response.PaymentStatusResponse;
import com.shopee.ecommerce.module.payment.dto.response.RefundResponse;
import com.shopee.ecommerce.module.payment.entity.Payment;
import org.mapstruct.*;

/**
 * MapStruct mapper: Payment entity → response DTOs.
 */
@Mapper(
    componentModel       = "spring",
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentMapper {

    // ── Payment → InitiatePaymentResponse ────────────────────────────────────

    @Mapping(target = "paymentId",  source = "payment.id")
    @Mapping(target = "deeplink",   ignore = true)
    InitiatePaymentResponse toInitiateResponse(Payment payment);

    /** Overload that also carries the deeplink from the PayPay create response. */
    default InitiatePaymentResponse toInitiateResponse(Payment payment, String deeplink) {
        InitiatePaymentResponse base = toInitiateResponse(payment);
        return InitiatePaymentResponse.builder()
                .paymentId(base.getPaymentId())
                .merchantPaymentId(base.getMerchantPaymentId())
                .orderId(base.getOrderId())
                .amount(base.getAmount())
                .currency(base.getCurrency())
                .status(base.getStatus())
                .paymentUrl(base.getPaymentUrl())
                .deeplink(deeplink)
                .expiresAt(base.getExpiresAt())
                .createdAt(base.getCreatedAt())
                .build();
    }

    // ── Payment → PaymentStatusResponse ──────────────────────────────────────

    @Mapping(target = "paymentId",   source = "id")
    @Mapping(target = "statusLabel", expression = "java(payment.getStatus().name())")
    PaymentStatusResponse toStatusResponse(Payment payment);

    // ── Payment → RefundResponse ──────────────────────────────────────────────

    @Mapping(target = "paymentId",      source = "id")
    @Mapping(target = "refundedAmount", source = "amount")
    @Mapping(target = "paymentStatus",  source = "status")
    RefundResponse toRefundResponse(Payment payment);
}
