package com.shopee.ecommerce.module.order.dto.request;

import com.shopee.ecommerce.module.order.entity.OrderStatus;
import jakarta.validation.constraints.*;
import lombok.*;

// ═══════════════════════════════════════════════════════════════════════════
//  CancelOrderRequest  — POST /api/orders/{id}/cancel
// ═══════════════════════════════════════════════════════════════════════════

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 5, max = 500, message = "Reason must be 5–500 characters")
    private String reason;
}
