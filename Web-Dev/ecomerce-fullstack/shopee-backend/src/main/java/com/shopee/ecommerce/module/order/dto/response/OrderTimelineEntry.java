package com.shopee.ecommerce.module.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Each entry in the buyer-facing order progress timeline.
 * The frontend renders this as a vertical stepper component.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTimelineEntry {

    private OrderStatus   status;
    private String        label;
    private String        description;
    private LocalDateTime completedAt;
    private boolean       completed;
    private boolean       current;
}