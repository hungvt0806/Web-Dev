package com.shopee.ecommerce.module.order.dto.request;

import com.shopee.ecommerce.module.order.entity.OrderStatus;
import jakarta.validation.constraints.*;
import lombok.*;

// ═══════════════════════════════════════════════════════════════════════════
//  UpdateOrderStatusRequest  — PATCH /api/admin/orders/{id}/status
// ═══════════════════════════════════════════════════════════════════════════

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Target status is required")
    private OrderStatus status;

    @Size(max = 500)
    private String note;

    /** Required when transitioning to SHIPPED. */
    @Size(max = 100)
    private String trackingNumber;

    @Size(max = 100)
    private String shippingCarrier;
}
