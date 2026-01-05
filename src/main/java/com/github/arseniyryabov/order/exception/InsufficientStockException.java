package com.github.arseniyryabov.order.exception;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(UUID productId, Integer available, Integer requested) {
        super(String.format("Недостаточно товара с ID %s. Доступно: %d, Запрошено: %d",
                productId, available, requested));
    }
}
