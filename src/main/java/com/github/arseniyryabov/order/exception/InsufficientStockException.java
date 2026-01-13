package com.github.arseniyryabov.order.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId, Integer available, Integer requested) {
        super("Недостаточно товара " + productId +
                ". Доступно: " + available + ", запрошено: " + requested);
    }
}
