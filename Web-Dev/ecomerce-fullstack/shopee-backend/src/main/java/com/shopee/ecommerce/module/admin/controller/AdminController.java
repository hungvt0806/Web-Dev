package com.shopee.ecommerce.module.admin.controller;

import com.shopee.ecommerce.module.order.repository.OrderRepository;
import com.shopee.ecommerce.module.product.repository.ProductRepository;
import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin dashboard — ROLE_ADMIN required")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Get KPI stats: revenue, orders, users, products")
    public ResponseEntity<ApiResponse<?>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();

        // Orders
        stats.put("totalOrders", orderRepository.count());
        stats.put("ordersByStatus", orderRepository.countGroupedByStatus());

        // Revenue (last 30 days)
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to   = LocalDateTime.now();
        stats.put("revenueThisMonth", orderRepository.sumRevenueBetween(from, to));

        // Products
        stats.put("totalProducts", productRepository.count());

        // Users
        stats.put("totalUsers", userRepository.count());
        stats.put("totalRegularUsers", userRepository.countByRole(User.Role.USER));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<?>> updateOrderStatus(
            @PathVariable UUID id, @RequestParam String status) {
        // Delegate to AdminOrderController if exists, otherwise stub
        return ResponseEntity.ok(ApiResponse.success("Use /api/admin/orders/" + id + "/status instead"));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users with pagination")
    public ResponseEntity<ApiResponse<?>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String search
    ) {
        Page<User> users = (search != null && !search.isBlank())
                ? userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                search, search, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                : userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return ResponseEntity.ok(ApiResponse.ofPage(users));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<ApiResponse<?>> updateUserStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.shopee.ecommerce.exception.ResourceNotFoundException("User", "id", id));
        user.setActive(active);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User status updated"));
    }
}