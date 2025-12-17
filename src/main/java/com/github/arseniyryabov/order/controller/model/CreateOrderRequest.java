package com.github.arseniyryabov.order.controller.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Адрес обязателен")
    private String address;

    @NotNull(message = "Способ доставки обязателен")
    private String deliveryMethod;

    @NotEmpty(message = "Список товаров не может быть пустым")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    @NoArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "ID продукта обязателен")
        private UUID productId;

        @NotNull(message = "Количество обязательно")
        private Integer quantity;
    }
}

