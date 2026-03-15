package com.shopee.ecommerce.module.order.service;

import com.shopee.ecommerce.exception.*;
import com.shopee.ecommerce.module.cart.entity.Cart;
import com.shopee.ecommerce.module.cart.entity.CartItem;
import com.shopee.ecommerce.module.cart.repository.CartItemRepository;
import com.shopee.ecommerce.module.cart.repository.CartRepository;
import com.shopee.ecommerce.module.order.dto.request.CancelOrderRequest;
import com.shopee.ecommerce.module.order.dto.request.PlaceOrderRequest;
import com.shopee.ecommerce.module.order.dto.request.UpdateOrderStatusRequest;
import com.shopee.ecommerce.module.order.dto.response.OrderDetailResponse;
import com.shopee.ecommerce.module.order.dto.response.OrderSummaryResponse;
import com.shopee.ecommerce.module.order.entity.*;
import com.shopee.ecommerce.module.order.entity.OrderStatusHistory.ActorType;
import com.shopee.ecommerce.module.order.mapper.OrderMapper;
import com.shopee.ecommerce.module.order.repository.OrderItemRepository;
import com.shopee.ecommerce.module.order.repository.OrderRepository;
import com.shopee.ecommerce.module.order.statemachine.OrderStateMachine;
import com.shopee.ecommerce.module.product.entity.Product;
import com.shopee.ecommerce.module.product.entity.ProductVariant;
import com.shopee.ecommerce.module.product.repository.ProductRepository;
import com.shopee.ecommerce.module.product.repository.ProductVariantRepository;
import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order service — all order business logic lives here.
 *
 * Key design decisions:
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ Snapshot pattern: product name, image, price are copied at          │
 * │   placement time so order history survives product edits/deletions  │
 * │                                                                     │
 * │ Atomic stock decrement: uses a WHERE stock >= qty UPDATE to         │
 * │   prevent overselling without a SELECT FOR UPDATE lock              │
 * │                                                                     │
 * │ State machine: all status changes go through OrderStateMachine      │
 * │   which enforces the transition graph and appends history entries   │
 * │                                                                     │
 * │ Cart cleared only after the order is fully persisted; a failure     │
 * │   mid-transaction rolls back the order and leaves the cart intact   │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("500"); // ¥500

    private final OrderRepository        orderRepository;
    private final OrderItemRepository    orderItemRepository;
    private final CartRepository         cartRepository;
    private final CartItemRepository     cartItemRepository;
    private final ProductRepository      productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository         userRepository;
    private final OrderNumberGenerator   orderNumberGenerator;
    private final OrderMapper            orderMapper;

    // ═══════════════════════════════════════════════════════════════════════
    //  PLACE ORDER
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderDetailResponse placeOrder(UUID buyerId, PlaceOrderRequest request) {

        // 1. Load buyer ─────────────────────────────────────────────────────
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", buyerId));

        // 2. Load cart ──────────────────────────────────────────────────────
        Cart cart = cartRepository.findByUserIdWithItems(buyerId)
                .orElseThrow(() -> new EmptyCartException("Cart is empty or not found"));

        if (cart.isEmpty()) {
            throw new EmptyCartException("Cannot place an order with an empty cart");
        }

        // 3. Validate items and build snapshots ─────────────────────────────
        List<CartItem> cartItems = cart.getItems();
        List<StockReservation> reservations = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem ci : cartItems) {
            Product product = ci.getProduct();

            // Validate product is still active
            if (product.getStatus() != Product.ProductStatus.ACTIVE) {
                throw new BusinessException(
                        "'" + product.getName() + "' is no longer available for purchase.");
            }

            // Validate and reserve stock
            if (ci.getVariant() != null) {
                ProductVariant v = ci.getVariant();
                if (!v.isActive()) {
                    throw new BusinessException(
                            "Selected variant of '" + product.getName() + "' is unavailable.");
                }
                if (v.getStock() < ci.getQuantity()) {
                    throw new InsufficientStockException(
                            product.getName(), ci.getQuantity(), v.getStock());
                }
                reservations.add(new StockReservation(null, v.getId(), ci.getQuantity()));
            } else {
                if (product.getTotalStock() < ci.getQuantity()) {
                    throw new InsufficientStockException(
                            product.getName(), ci.getQuantity(), product.getTotalStock());
                }
                reservations.add(new StockReservation(product.getId(), null, ci.getQuantity()));
            }

            subtotal = subtotal.add(
                    ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // 4. Calculate totals ────────────────────────────────────────────────
        BigDecimal shippingFee    = calculateShippingFee(subtotal);
        BigDecimal discountAmount = applyOptionalCoupon(request.getCouponCode(), subtotal);
        BigDecimal totalAmount    = subtotal.add(shippingFee).subtract(discountAmount);

        // 5. Build Order aggregate ───────────────────────────────────────────
        String orderNumber = orderNumberGenerator.generate();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .shippingAddress(request.getShippingAddress().toMap())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .currency("JPY")
                .buyerNote(request.getBuyerNote())
                .couponCode(request.getCouponCode())
                .status(OrderStatus.PENDING)
                .build();

        // Initial history entry
        OrderStateMachine.systemTransition(order, OrderStatus.PENDING, "Order placed by buyer");

        // 6. Build OrderItems (snapshot) ─────────────────────────────────────
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            ProductVariant v = ci.getVariant();

            OrderItem item = OrderItem.builder()
                    .productId(p.getId())
                    .variantId(v != null ? v.getId() : null)
                    .productName(p.getName())
                    .productImage(p.getThumbnailUrl())
                    .sku(v != null ? v.getSku() : null)
                    .variantAttributes(v != null ? v.getAttributes() : null)
                    .unitPrice(ci.getUnitPrice())
                    .quantity(ci.getQuantity())
                    .build();
            item.computeLineTotal();
            order.addItem(item);
        }

        // 7. Persist order ───────────────────────────────────────────────────
        Order saved = orderRepository.save(order);

        // 8. Decrement stock atomically ───────────────────────────────────────
        for (StockReservation r : reservations) {
            if (r.variantId() != null) {
                int updated = variantRepository.decrementStock(r.variantId(), r.quantity());
                if (updated == 0) {
                    // Race condition — another order grabbed the last stock
                    throw new InsufficientStockException(
                            "A product ran out of stock while placing your order. Please try again.");
                }
            } else {
                int updated = productRepository.decrementStock(r.productId(), r.quantity());
                if (updated == 0) {
                    throw new InsufficientStockException(
                            "A product ran out of stock while placing your order. Please try again.");
                }
            }
        }

        // 9. Clear the cart ──────────────────────────────────────────────────
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order placed: {} (buyer={}, total=¥{})", orderNumber, buyerId, totalAmount);

        // 10. Return full detail ─────────────────────────────────────────────
        return orderMapper.toDetail(
                orderRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GET ORDER HISTORY
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrderHistory(UUID buyerId,
                                                       String statusFilter,
                                                       Pageable pageable) {
        OrderStatus status = parseStatus(statusFilter);
        Page<Order> page = (status != null)
                ? orderRepository.findByBuyerIdAndStatus(buyerId, status, pageable)
                : orderRepository.findByBuyerId(buyerId, pageable);

        return page.map(orderMapper::toSummary);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GET ORDER DETAIL
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(UUID orderId, UUID buyerId) {
        Order order = orderRepository.findByIdAndBuyerIdWithDetails(orderId, buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return orderMapper.toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderByNumber(String orderNumber, UUID buyerId) {
        Order order = orderRepository.findByOrderNumberAndBuyerId(orderNumber, buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return orderMapper.toDetail(order);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CANCEL ORDER (BUYER)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(UUID orderId, UUID buyerId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.isOwnedBy(buyerId)) {
            throw new AccessDeniedException("Order " + orderId + " does not belong to this user");
        }
        if (!order.isCancellableByBuyer()) {
            throw new InvalidOrderTransitionException(
                    "Order " + order.getOrderNumber() +
                    " cannot be cancelled (current status: " + order.getStatus() + "). " +
                    "Only PENDING or AWAITING_PAYMENT orders may be cancelled by the buyer.");
        }

        order.setCancellationReason(request.getReason());
        OrderStateMachine.transition(order, OrderStatus.CANCELLED,
                buyerId, ActorType.BUYER, "Cancelled by buyer: " + request.getReason());

        restoreStock(order.getItems());
        Order saved = orderRepository.save(order);

        log.info("Order cancelled: {} (buyer={}, reason={})",
                order.getOrderNumber(), buyerId, request.getReason());

        return orderMapper.toDetail(
                orderRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN: LIST ALL ORDERS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listAllOrders(String statusFilter, Pageable pageable) {
        OrderStatus status = parseStatus(statusFilter);
        return orderRepository.findAllForAdmin(status, pageable).map(orderMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetailForAdmin(UUID orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return orderMapper.toDetail(order);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN: UPDATE ORDER STATUS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(UUID orderId,
                                                  UpdateOrderStatusRequest request,
                                                  UUID adminId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus target = request.getStatus();

        // Attach tracking info when shipping
        if (target == OrderStatus.SHIPPED) {
            if (!StringUtils.hasText(request.getTrackingNumber())) {
                throw new BusinessException("Tracking number is required when marking an order as SHIPPED");
            }
            order.setTrackingNumber(request.getTrackingNumber());
            order.setShippingCarrier(request.getShippingCarrier());
        }

        // Restore stock on admin cancellation
        if (target == OrderStatus.CANCELLED) {
            order.setCancellationReason(request.getNote());
            restoreStock(order.getItems());
        }

        OrderStateMachine.transition(order, target, adminId, ActorType.ADMIN, request.getNote());
        Order saved = orderRepository.save(order);

        log.info("Order status updated: {} → {} by admin={}", order.getOrderNumber(), target, adminId);

        return orderMapper.toDetail(
                orderRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Restore stock for all items in a cancelled order.
     * Increments the product or variant stock for each order item.
     */
    private void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getVariantId() != null) {
                variantRepository.incrementStock(item.getVariantId(), item.getQuantity());
            } else if (item.getProductId() != null) {
                productRepository.incrementStock(item.getProductId(), item.getQuantity());
            }
        }
    }

    /**
     * Flat-rate shipping fee logic.
     * Orders over ¥3,000 get free shipping.
     */
    private BigDecimal calculateShippingFee(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("3000")) >= 0
                ? BigDecimal.ZERO
                : FLAT_SHIPPING_FEE;
    }

    /**
     * Coupon application stub.
     * Returns ¥0 discount for now — replace with real coupon repository lookup.
     */
    private BigDecimal applyOptionalCoupon(String couponCode, BigDecimal subtotal) {
        if (!StringUtils.hasText(couponCode)) return BigDecimal.ZERO;
        // TODO: look up coupon in coupon_codes table, validate, apply
        log.debug("Coupon '{}' applied (stub — returning ¥0 discount)", couponCode);
        return BigDecimal.ZERO;
    }

    /** Parse a status string to an OrderStatus enum, returning null for blank/invalid values. */
    private OrderStatus parseStatus(String s) {
        if (!StringUtils.hasText(s)) return null;
        try {
            return OrderStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid order status filter: '" + s + "'");
        }
    }

    /** Internal value object used during the stock-reservation phase of placeOrder. */
    private record StockReservation(Long productId, Long variantId, int quantity) {}
}
