package com.github.arseniyryabov.order.integration.client;

import com.github.arseniyryabov.order.integration.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


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

    // Проверка товара
    public boolean isProductExists(UUID productId) {
        try {
            productServiceRestClient.get()
                    .uri("/products/{id}", productId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при проверке товара: " + e.getMessage(), e);
        }
    }

    // Проверка наличия товара
    public boolean isProductAvailable(UUID productId, Integer quantity) {
        try {
            ProductResponse product = getProductById(productId);
            if (product == null) {
                return false;
            }
            return product.getStockQuantity() >= quantity;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при проверке наличия товара: " + e.getMessage());
        }
    }

    // Уменьшение кол-ва товара на складе
    public void decreaseProductStock(UUID productId, Integer quantity) {
        try {
            productServiceRestClient.post()
                    .uri("/products/{productId}/decrease-stock", productId)
                    .body(Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при уменьшении количества товара: " + e.getMessage());
        }
    }

    // Получение информации о наличии товара партией
    public Map<UUID, Integer> getProductsStockInfo(List<UUID> productIds) {
        try {
            List<ProductResponse> products = getProductsByIds(productIds);
            return products.stream()
                    .collect(Collectors.toMap(
                            ProductResponse::getProductId,
                            ProductResponse::getStockQuantity
                    ));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении информации о наличии товаров: " + e.getMessage());
        }
    }
}
