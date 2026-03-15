package com.shopee.ecommerce.module.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class InitiatePaymentRequest {
    @NotNull(message = "Order ID is required")
    private UUID orderId;
}
