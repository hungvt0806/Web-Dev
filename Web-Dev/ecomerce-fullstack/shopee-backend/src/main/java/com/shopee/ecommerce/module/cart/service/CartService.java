package com.shopee.ecommerce.module.cart.service;

import com.shopee.ecommerce.module.cart.entity.Cart;
import java.util.UUID;

public interface CartService {
    Cart getCart(UUID userId);
    Cart addItem(UUID userId, Long productId, Long variantId, int quantity);
    Cart updateItem(UUID userId, Long cartItemId, int quantity);
    Cart removeItem(UUID userId, Long cartItemId);
    void clearCart(UUID userId);
}