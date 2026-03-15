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
public class RefundResponse {
    private UUID          paymentId;
    private UUID          orderId;
    private BigDecimal    refundedAmount;
    private String        currency;
    private PaymentStatus paymentStatus;
    private LocalDateTime refundedAt;
}