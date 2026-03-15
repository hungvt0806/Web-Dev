package com.shopee.ecommerce.module.order.service;

import com.shopee.ecommerce.module.order.dto.request.CancelOrderRequest;
import com.shopee.ecommerce.module.order.dto.request.PlaceOrderRequest;
import com.shopee.ecommerce.module.order.dto.request.UpdateOrderStatusRequest;
import com.shopee.ecommerce.module.order.dto.response.OrderDetailResponse;
import com.shopee.ecommerce.module.order.dto.response.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Order service contract.
 *
 * Buyer operations: place, view history, view detail, cancel.
 * Admin operations: view all, update status (ship, deliver, etc.).
 *
 * All methods that place or mutate an order return the full
 * OrderDetailResponse so the frontend can re-render without a
 * second GET.
 */
public interface OrderService {

    // ── Buyer operations ──────────────────────────────────────────────────────

    /**
     * Place a new order from the authenticated buyer's current cart.
     *
     * Steps:
     *  1. Load buyer's cart — fail if empty
     *  2. Validate every item (product ACTIVE, variant ACTIVE, stock ≥ qty)
     *  3. Build Order + OrderItems (snapshot product data)
     *  4. Calculate totals (subtotal, shipping, optional coupon discount)
     *  5. Decrement stock atomically for each variant / product
     *  6. Persist Order (status = PENDING)
     *  7. Clear the buyer's cart
     *  8. Return OrderDetailResponse
     *
     * @throws com.shopee.ecommerce.exception.EmptyCartException      if the cart is empty
     * @throws com.shopee.ecommerce.exception.InsufficientStockException if any item has qty > stock
     * @throws com.shopee.ecommerce.exception.BusinessException        for other validation failures
     */
    OrderDetailResponse placeOrder(UUID buyerId, PlaceOrderRequest request);

    /**
     * Paginated order history for a buyer, newest first.
     * Optionally filtered by status (e.g. "show only SHIPPED orders").
     */
    Page<OrderSummaryResponse> getOrderHistory(UUID buyerId,
                                               String statusFilter,
                                               Pageable pageable);

    /**
     * Full order detail — buyer must own the order.
     *
     * @throws com.shopee.ecommerce.exception.ResourceNotFoundException if order not found or not owned
     */
    OrderDetailResponse getOrderDetail(UUID orderId, UUID buyerId);

    /**
     * Look up an order by its human-readable order number.
     * Used in the "Track my order" flow.
     */
    OrderDetailResponse getOrderByNumber(String orderNumber, UUID buyerId);

    /**
     * Buyer-initiated cancellation (only PENDING and AWAITING_PAYMENT are cancellable).
     *
     * Restores reserved stock for all items.
     *
     * @throws com.shopee.ecommerce.exception.InvalidOrderTransitionException if not cancellable
     */
    OrderDetailResponse cancelOrder(UUID orderId, UUID buyerId, CancelOrderRequest request);

    // ── Admin / seller operations ─────────────────────────────────────────────

    /**
     * Admin: paginated list of all orders, optionally filtered by status.
     */
    Page<OrderSummaryResponse> listAllOrders(String statusFilter, Pageable pageable);

    /**
     * Admin: full detail of any order (no buyer ownership check).
     */
    OrderDetailResponse getOrderDetailForAdmin(UUID orderId);

    /**
     * Admin / seller: advance or change an order's status.
     *
     * Handles side-effects per transition:
     *  → SHIPPED:   saves trackingNumber + shippingCarrier, stamps shippedAt
     *  → CANCELLED: restores reserved stock
     *  → REFUNDED:  (stub — full refund workflow is a separate feature)
     *
     * @throws com.shopee.ecommerce.exception.InvalidOrderTransitionException if transition is invalid
     */
    OrderDetailResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request,
                                          UUID adminId);
}
