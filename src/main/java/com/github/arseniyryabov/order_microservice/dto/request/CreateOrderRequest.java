package com.github.arseniyryabov.order_microservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Адрес не может быть пустым")
    private String address;

    @NotBlank(message = "Способ доставки не может быть пустым")
    private String deliveryMethod;

    @NotNull(message = "Список товаров не может быть пустым")
    private List<OrderItemRequest> items;

    @NotNull(message = "Общая сумма не может быть пустой")
    @Positive(message = "Общая сумма должна быть положительной")
    private BigDecimal totalAmount;
}

