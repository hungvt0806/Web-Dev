package com.shopee.ecommerce.module.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


// ═══════════════════════════════════════════════════════════════════════════
//  OrderSummaryResponse — used in list/history pages (no items loaded)
// ═══════════════════════════════════════════════════════════════════════════

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSummaryResponse {

    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private String statusLabel;      // e.g. "On the Way"

    private int totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;

    /**
     * Thumbnail of the first item — shown on the order card.
     */
    private String firstItemImage;
    private String firstItemName;
    private int additionalItemCount;  // "and 3 more items"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
