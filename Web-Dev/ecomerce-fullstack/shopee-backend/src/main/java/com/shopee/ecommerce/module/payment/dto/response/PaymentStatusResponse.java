package com.shopee.ecommerce.module.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusResponse {
    private UUID          paymentId;
    private String        merchantPaymentId;
    private String        paypayPaymentId;
    private UUID          orderId;
    private PaymentStatus status;
    private String        statusLabel;
    private BigDecimal    amount;
    private String        currency;
    private String        paymentMethod;
    private LocalDateTime authorizedAt;
    private LocalDateTime capturedAt;
    private LocalDateTime failedAt;
    private String        failureReason;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}