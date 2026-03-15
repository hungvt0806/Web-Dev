package com.shopee.ecommerce.module.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailResponse {

    private UUID        id;
    private String      orderNumber;
    private OrderStatus status;
    private String      statusLabel;

    private List<OrderItemResponse>          items;
    private List<OrderStatusHistoryResponse> statusHistory;
    private List<OrderTimelineEntry>         timeline;

    private Map<String, String> shippingAddress;
    private String              trackingNumber;
    private String              shippingCarrier;
    private LocalDateTime       estimatedDeliveryDate;

    private BigDecimal  subtotal;
    private BigDecimal  shippingFee;
    private BigDecimal  discountAmount;
    private BigDecimal  totalAmount;
    private String      currency;
    private String      couponCode;

    private String      buyerNote;
    private BuyerInfo   buyer;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
}