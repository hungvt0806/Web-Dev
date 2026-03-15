package com.shopee.ecommerce.module.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

// ═══════════════════════════════════════════════════════════════════════════
//  PlaceOrderRequest  — POST /api/orders
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Payload to place an order from the buyer's current cart.
 *
 * The cart is read server-side using the authenticated user's ID —
 * the frontend never sends the cart contents, preventing price tampering.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddressRequest shippingAddress;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String buyerNote;

    /** Optional coupon code to apply to this order. */
    @Size(max = 50)
    private String couponCode;

    // ── Nested ────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ShippingAddressRequest {

        @NotBlank(message = "Full name is required")
        @Size(max = 200)
        private String fullName;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[+\\d][\\d\\s\\-]{6,19}$", message = "Invalid phone number")
        private String phone;

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 300)
        private String addressLine1;

        @Size(max = 300)
        private String addressLine2;

        @NotBlank(message = "City is required")
        @Size(max = 100)
        private String city;

        @Size(max = 100)
        private String province;

        @NotBlank(message = "Postal code is required")
        @Size(max = 20)
        private String postalCode;

        @NotBlank(message = "Country is required")
        @Size(max = 2, min = 2, message = "Country must be a 2-letter ISO code")
        private String country;

        /** Convert to a flat Map for JSONB storage on the Order entity. */
        public Map<String, String> toMap() {
            var map = new java.util.LinkedHashMap<String, String>();
            map.put("fullName",     fullName);
            map.put("phone",        phone);
            map.put("addressLine1", addressLine1);
            if (addressLine2 != null) map.put("addressLine2", addressLine2);
            map.put("city",         city);
            if (province != null)   map.put("province", province);
            map.put("postalCode",   postalCode);
            map.put("country",      country);
            return map;
        }
    }
}
