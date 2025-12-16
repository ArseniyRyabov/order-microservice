package com.github.arseniyryabov.order_microservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "ID товара не может быть пустым")
    private String productId;

    @NotNull(message = "Количество не может быть пустым")
    @Positive(message = "Количество должно быть положительным")
    private Integer quantity;
}
