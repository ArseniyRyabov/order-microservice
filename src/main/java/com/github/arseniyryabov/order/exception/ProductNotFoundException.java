package com.github.arseniyryabov.order.exception;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super("Продукт с ID " + productId + " не найден");
    }
}
