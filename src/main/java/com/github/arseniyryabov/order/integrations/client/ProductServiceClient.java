package com.github.arseniyryabov.order.integrations.client;

import com.github.arseniyryabov.order.integrations.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestClient productServiceRestClient;

    public ProductResponse getProductById(UUID productId) {
        try {
            return productServiceRestClient.get()
                    .uri("/products/{productId}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении продукта: " + e.getMessage());
        }
    }

    public List<ProductResponse> getProductsByIds(List<UUID> productIds) {
        try {
            return productServiceRestClient.post()
                    .uri("/products/batch")
                    .body(productIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ProductResponse>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении продуктов: " + e.getMessage());
        }
    }

    public BigDecimal getProductPrice(UUID productId) {
        ProductResponse product = getProductById(productId);
        if (product == null) {
            throw new RuntimeException("Продукт с ID " + productId + " не найден");
        }
        return product.getPrice();
    }
}
