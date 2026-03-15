package com.shopee.ecommerce.module.cart.service;

import com.shopee.ecommerce.exception.ResourceNotFoundException;
import com.shopee.ecommerce.module.cart.entity.Cart;
import com.shopee.ecommerce.module.cart.entity.CartItem;
import com.shopee.ecommerce.module.cart.repository.CartItemRepository;
import com.shopee.ecommerce.module.cart.repository.CartRepository;
import com.shopee.ecommerce.module.product.entity.Product;
import com.shopee.ecommerce.module.product.entity.ProductVariant;
import com.shopee.ecommerce.module.product.repository.ProductRepository;
import com.shopee.ecommerce.module.product.repository.ProductVariantRepository;
import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository        cartRepository;
    private final CartItemRepository    cartItemRepository;
    private final ProductRepository     productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository        userRepository;

    @Override
    @Transactional(readOnly = true)
    public Cart getCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCart(userId));
    }

    @Override
    public Cart addItem(UUID userId, Long productId, Long variantId, int quantity) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCart(userId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        ProductVariant variant = null;
        if (variantId != null) {
            variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", variantId));
        }

        // Check if item already exists — update quantity
        final Long finalVariantId = variantId;
        java.util.Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId)
                        && (finalVariantId == null || (i.getVariant() != null && i.getVariant().getId().equals(finalVariantId))))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(quantity)
                    .unitPrice(variant != null ? variant.getPrice() : product.getBasePrice())
                    .build();
            cart.getItems().add(item);
        }

        return cartRepository.save(cart);
    }

    @Override
    public Cart updateItem(UUID userId, Long cartItemId, int quantity) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }

        return cartRepository.save(cart);
    }

    @Override
    public Cart removeItem(UUID userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
        return cartRepository.save(cart);
    }

    @Override
    public void clearCart(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteAllByCartId(cart.getId());
        });
    }

    private Cart createCart(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Cart cart = Cart.builder().user(user).build();
        return cartRepository.save(cart);
    }
}