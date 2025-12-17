package com.github.arseniyryabov.order.controller.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private Long userId;
    private String address;
    private String deliveryMethod;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemResponse {
        private UUID orderItemId;
        private UUID productId;
        private Integer quantity;
    }
}
