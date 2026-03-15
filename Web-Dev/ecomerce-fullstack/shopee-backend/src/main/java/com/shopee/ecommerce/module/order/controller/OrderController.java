package com.shopee.ecommerce.module.order.controller;

import com.shopee.ecommerce.module.order.dto.request.CancelOrderRequest;
import com.shopee.ecommerce.module.order.dto.request.PlaceOrderRequest;
import com.shopee.ecommerce.module.order.dto.response.OrderDetailResponse;
import com.shopee.ecommerce.module.order.dto.response.OrderSummaryResponse;
import com.shopee.ecommerce.module.order.service.OrderService;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Buyer-facing order controller.
 *
 * Base path: /api/orders
 * All endpoints require authentication (JWT).
 *
 * ┌───────────────────────────────────────────────────────────────────┐
 * │  POST   /api/orders                — place order from cart        │
 * │  GET    /api/orders                — order history (paginated)    │
 * │  GET    /api/orders/{id}           — order detail by UUID         │
 * │  GET    /api/orders/number/{num}   — order detail by order number │
 * │  POST   /api/orders/{id}/cancel    — cancel an order              │
 * └───────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, history, and tracking")
public class OrderController {

    private final OrderService orderService;

    // ── Place order ───────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary     = "Place an order",
        description = "Creates a new order from the authenticated buyer's cart. " +
                      "Validates stock, snapshots product data, decrements inventory, " +
                      "and clears the cart on success."
    )
    public ResponseEntity<ApiResponse<OrderDetailResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderDetailResponse order = orderService.placeOrder(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    // ── Order history ─────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
        summary     = "Get order history",
        description = "Paginated list of the authenticated buyer's orders, newest first. " +
                      "Use the 'status' parameter to filter by a specific order status."
    )
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> getHistory(
            @Parameter(description = "Filter by status: PENDING | PAID | SHIPPED | DELIVERED | CANCELLED")
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryResponse> result = orderService.getOrderHistory(
                principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ofPage(result));
    }

    // ── Order detail ──────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(
        summary     = "Get order detail by ID",
        description = "Returns full order details including all items, status history, " +
                      "and a display-ready tracking timeline."
    )
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderDetail(id, principal.getId())));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(
        summary     = "Get order detail by order number",
        description = "Look up an order using the human-readable order number " +
                      "(e.g. ORD-20240315-A3F2K9). Used in the 'Track my order' flow."
    )
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderByNumber(orderNumber, principal.getId())));
    }

    // ── Cancel order ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @Operation(
        summary     = "Cancel an order",
        description = "Cancels an order. Only PENDING and AWAITING_PAYMENT orders " +
                      "can be cancelled by the buyer. Stock is restored on success."
    )
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderDetailResponse order = orderService.cancelOrder(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", order));
    }
}
