package com.shopee.ecommerce.module.order.controller;

import com.shopee.ecommerce.module.order.dto.request.UpdateOrderStatusRequest;
import com.shopee.ecommerce.module.order.dto.response.OrderDetailResponse;
import com.shopee.ecommerce.module.order.dto.response.OrderSummaryResponse;
import com.shopee.ecommerce.module.order.service.OrderService;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin order management controller.
 *
 * Base path: /api/admin/orders
 * Requires ROLE_ADMIN or ROLE_SELLER.
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  GET   /api/admin/orders          — list all orders (filterable)   │
 * │  GET   /api/admin/orders/{id}     — any order detail               │
 * │  PATCH /api/admin/orders/{id}/status — update order status         │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Admin: Orders", description = "Order management for admins and sellers")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(
        summary     = "List all orders",
        description = "Paginated list of all orders across all buyers. " +
                      "Filter by status to view e.g. all PAID orders awaiting processing."
    )
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderSummaryResponse> result = orderService.listAllOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.ofPage(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any order detail (admin, no ownership check)")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderDetailForAdmin(id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
        summary     = "Update order status",
        description = "Advance or change an order's status. " +
                      "Tracking number is required when setting status to SHIPPED. " +
                      "Stock is automatically restored when cancelling."
    )
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderDetailResponse order = orderService.updateOrderStatus(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated to " + request.getStatus(), order));
    }
}
