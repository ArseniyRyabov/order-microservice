package com.github.arseniyryabov.order.exception;

import java.util.UUID;

public class ProductUnavailableException extends RuntimeException {

    public ProductUnavailableException(UUID productId) {
        super(String.format("Товар с ID %s временно отсутствует на складе", productId));
    }
}
