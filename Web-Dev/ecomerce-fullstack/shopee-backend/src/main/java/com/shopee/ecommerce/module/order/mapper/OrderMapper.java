package com.shopee.ecommerce.module.order.mapper;

import com.shopee.ecommerce.module.order.dto.response.*;
import com.shopee.ecommerce.module.order.entity.*;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MapStruct mapper: Order entity → response DTOs.
 *
 * The timeline is built in an @AfterMapping hook because it requires
 * inspecting multiple timestamp fields on the Order and cannot be
 * expressed as a simple field-to-field mapping.
 */
@Mapper(
    componentModel       = "spring",
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

    // ── Order → OrderSummaryResponse ─────────────────────────────────────────

    @Mapping(target = "statusLabel",          expression = "java(order.getStatus().displayLabel())")
    @Mapping(target = "totalQuantity",        expression = "java(order.getTotalQuantity())")
    @Mapping(target = "firstItemImage",       ignore = true)
    @Mapping(target = "firstItemName",        ignore = true)
    @Mapping(target = "additionalItemCount",  ignore = true)
    OrderSummaryResponse toSummary(Order order);

    @AfterMapping
    default void enrichSummary(@MappingTarget OrderSummaryResponse.OrderSummaryResponseBuilder b,
                                Order order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem first = order.getItems().get(0);
            b.firstItemImage(first.getProductImage())
             .firstItemName(first.getProductName())
             .additionalItemCount(Math.max(0, order.getItems().size() - 1));
        }
    }

    List<OrderSummaryResponse> toSummaryList(List<Order> orders);

    // ── Order → OrderDetailResponse ───────────────────────────────────────────

    @Mapping(target = "statusLabel",   expression = "java(order.getStatus().displayLabel())")
    @Mapping(target = "buyer.id",      source = "order.buyer.id")
    @Mapping(target = "buyer.email",   source = "order.buyer.email")
    @Mapping(target = "buyer.fullName",source = "order.buyer.fullName")
    @Mapping(target = "timeline",      ignore = true)  // built in @AfterMapping
    OrderDetailResponse toDetail(Order order);

    @AfterMapping
    default void buildTimeline(@MappingTarget OrderDetailResponse.OrderDetailResponseBuilder b,
                                Order order) {
        b.timeline(buildOrderTimeline(order));
    }

    // ── OrderItem → OrderItemResponse ─────────────────────────────────────────

    @Mapping(target = "displayName", expression = "java(item.getDisplayName())")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    // ── OrderStatusHistory → OrderStatusHistoryResponse ───────────────────────

    @Mapping(target = "toStatusLabel", expression = "java(history.getToStatus().displayLabel())")
    @Mapping(target = "actorType",     expression = "java(history.getActorType().name())")
    OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history);

    List<OrderStatusHistoryResponse> toHistoryResponseList(List<OrderStatusHistory> histories);

    // ── Timeline builder ──────────────────────────────────────────────────────

    /**
     * Build the buyer-facing progress timeline from the Order's status and timestamps.
     *
     * The timeline always shows all 5 steps in order. Steps before the current
     * status are marked completed; the current status is marked current;
     * future steps are pending.
     */
    default List<OrderTimelineEntry> buildOrderTimeline(Order order) {
        OrderStatus current = order.getStatus();
        List<OrderTimelineEntry> entries = new ArrayList<>();

        if (current == OrderStatus.CANCELLED) {
            entries.add(entry(OrderStatus.CANCELLED, "Order Cancelled",
                    "Your order has been cancelled.", order.getCancelledAt(), false, true));
            return entries;
        }
        if (current == OrderStatus.REFUNDED) {
            entries.add(entry(OrderStatus.REFUNDED, "Order Refunded",
                    "Your refund has been processed.", null, false, true));
            return entries;
        }

        // Normal happy-path timeline (5 steps)
        OrderStatus[] steps = {
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
        };
        String[] labels = {
            "Order Placed", "Payment Confirmed",
            "Preparing Shipment", "Shipped", "Delivered"
        };
        String[] descs = {
            "We received your order.",
            "Your payment has been verified.",
            "The seller is preparing your items.",
            "Your order is on its way.",
            "Your order has been delivered."
        };
        LocalDateTime[] times = {
            order.getCreatedAt(),
            order.getPaidAt(),
            null,
            order.getShippedAt(),
            order.getDeliveredAt()
        };

        int currentIdx = indexOf(steps, current);
        for (int i = 0; i < steps.length; i++) {
            boolean completed = i < currentIdx;
            boolean isCurrent = i == currentIdx;
            LocalDateTime ts  = completed || isCurrent ? times[i] : null;
            entries.add(entry(steps[i], labels[i], descs[i], ts, completed, isCurrent));
        }
        return entries;
    }

    private OrderTimelineEntry entry(OrderStatus status, String label, String desc,
                                      LocalDateTime completedAt,
                                      boolean completed, boolean current) {
        return OrderTimelineEntry.builder()
                .status(status).label(label).description(desc)
                .completedAt(completedAt).completed(completed).current(current)
                .build();
    }

    private int indexOf(OrderStatus[] arr, OrderStatus val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return 0;
    }
}
