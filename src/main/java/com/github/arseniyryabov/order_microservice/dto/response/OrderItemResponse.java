package com.github.arseniyryabov.order_microservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID orderItemId;
    private UUID productId;
    private Integer quantity;
}
