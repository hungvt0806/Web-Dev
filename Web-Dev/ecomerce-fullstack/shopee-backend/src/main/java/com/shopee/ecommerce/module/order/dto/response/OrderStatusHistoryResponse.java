package com.shopee.ecommerce.module.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopee.ecommerce.module.order.entity.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderStatusHistoryResponse {

    private Long        id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String      toStatusLabel;
    private String      note;
    private String      actorType;
    private LocalDateTime createdAt;
}