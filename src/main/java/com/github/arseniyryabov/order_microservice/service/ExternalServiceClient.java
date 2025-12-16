package com.github.arseniyryabov.order_microservice.service;

import com.github.arseniyryabov.order_microservice.dto.external.ProductResponse;
import com.github.arseniyryabov.order_microservice.dto.external.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

    private final WebClient userWebClient;
    private final WebClient productWebClient;

    public Mono<UserResponse> getUserById(Long userId) {
        log.info("Получение пользователя с ID: {}", userId);

        return userWebClient.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                return Mono.error(new RuntimeException("Пользователь не найден"));
                            }
                            return Mono.error(new RuntimeException("Ошибка при получении пользователя"));
                        })
                .bodyToMono(UserResponse.class)
                .doOnError(error -> log.error("Ошибка при получении пользователя: {}", error.getMessage()));
    }

    public Mono<ProductResponse> getProductById(UUID productId) {
        log.info("Получение продукта с ID: {}", productId);

        return productWebClient.get()
                .uri("/api/products/{productId}", productId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                                return Mono.error(new RuntimeException("Продукт не найден"));
                            }
                            return Mono.error(new RuntimeException("Ошибка при получении продукта"));
                        })
                .bodyToMono(ProductResponse.class)
                .doOnError(error -> log.error("Ошибка при получении продукта: {}", error.getMessage()));
    }

    public Flux<ProductResponse> getProductsByIds(List<UUID> productIds) {
        log.info("Получение продуктов по IDs: {}", productIds);

        return Flux.fromIterable(productIds)
                .flatMap(this::getProductById);
    }
}
