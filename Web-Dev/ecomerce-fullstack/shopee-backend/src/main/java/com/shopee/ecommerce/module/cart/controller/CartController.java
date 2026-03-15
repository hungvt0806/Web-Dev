package com.shopee.ecommerce.module.cart.controller;

import com.shopee.ecommerce.module.cart.service.CartService;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<?>> getCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.getCart(principal.getId())));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<?>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AddItemRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.addItem(principal.getId(), req.getProductId(),
                        req.getVariantId(), req.getQuantity())));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<?>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId,
            @RequestBody UpdateItemRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                cartService.updateItem(principal.getId(), itemId, req.getQuantity())));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<?>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId) {
        cartService.removeItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<?>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Data
    public static class AddItemRequest {
        private Long productId;
        private Long variantId;
        private int quantity = 1;
    }

    @Data
    public static class UpdateItemRequest {
        private int quantity;
    }
}