package com.github.arseniyryabov.order.controller;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.controller.model.OrderResponse;
import com.github.arseniyryabov.order.entity.OrderEntity;
import com.github.arseniyryabov.order.integration.client.ProductServiceClient;
import com.github.arseniyryabov.order.integration.client.UserServiceClient;
import com.github.arseniyryabov.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// REST контроллер для работы с заказами
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private final UserServiceClient userServiceClient;

    private final ProductServiceClient productServiceClient;

    // Метод для проверки подключения к user-service
    @GetMapping("/health/check-user-service")
    public ResponseEntity<String> checkUserServiceConnection() {
        try {
            boolean isAlive = userServiceClient.isUserExists(2L);
            return ResponseEntity.ok("User service is available. Connection successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("User service is unavailable: " + e.getMessage());
        }
    }

    // Метод для проверки подключения к product-service
    @GetMapping("/health/check-product-service")
    public ResponseEntity<String> checkProductServiceConnection() {
        try {
            boolean isAlive = productServiceClient.isProductExists(UUID.fromString("be87a6c6-aeef-463e-bb62-1b563f16db46"));
            return ResponseEntity.ok("User service is available. Connection successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Product service is unavailable: " + e.getMessage());
        }
    }

    // Создание заказа (POST /orders)
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        OrderEntity order = orderService.createOrder(request, userId);
        OrderResponse response = mapToResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Получение заказов пользователя (GET /orders)
    @GetMapping
    public List<OrderResponse> getUserOrders(
            @RequestHeader("X-User-Id") Long userId) {
        List<OrderEntity> orders = orderService.getUserOrders(userId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Получение конкретного заказа (GET /orders/{id})
    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") Long userId) {
        OrderEntity order = orderService.getOrderById(orderId, userId);
        return mapToResponse(order);
    }

    // Обновление статуса заказа (PATCH /orders/{id}/status)
    @PatchMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam String status) {
        OrderEntity order = orderService.updateOrderStatus(orderId, status);
        return mapToResponse(order);
    }

    // Получение заказов по статусу (GET /orders/status/{status})
    @GetMapping("/status/{status}")
    public List<OrderResponse> getOrdersByStatus(
            @PathVariable String status) {
        List<OrderEntity> orders = orderService.getOrdersByStatus(status);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Преобразование OrderEntity в OrderResponse (DTO)
    private OrderResponse mapToResponse(OrderEntity order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setUserId(order.getUserId());
        response.setAddress(order.getAddress());
        response.setDeliveryMethod(order.getDeliveryMethod());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getItems() != null) {
            List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                    .map(item -> {
                        OrderResponse.OrderItemResponse itemResponse =
                                new OrderResponse.OrderItemResponse();
                        itemResponse.setOrderItemId(item.getOrderItemId());
                        itemResponse.setProductId(item.getProductId());
                        itemResponse.setQuantity(item.getQuantity());
                        return itemResponse;
                    })
                    .collect(Collectors.toList());
            response.setItems(itemResponses);
        }

        return response;
    }
}
