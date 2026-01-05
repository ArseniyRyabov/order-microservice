package com.github.arseniyryabov.order.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductNotFoundException extends RuntimeException {

    private final UUID productId;

    public ProductNotFoundException(UUID productId) {
        super("Продукт с ID " + productId + " не найден");
        this.productId = productId;
    }

}
