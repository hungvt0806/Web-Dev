package com.shopee.ecommerce.module.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderItemResponse {

    private Long                id;
    private String              productName;
    private String              displayName;
    private String              productImage;
    private String              sku;
    private Map<String, String> variantAttributes;
    private BigDecimal          unitPrice;
    private int                 quantity;
    private BigDecimal          lineTotal;
    private boolean             reviewed;

    private Long                productId;
    private Long                variantId;
}