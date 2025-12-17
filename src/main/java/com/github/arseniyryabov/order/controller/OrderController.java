package com.github.arseniyryabov.order.controller;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.controller.model.OrderResponse;
import com.github.arseniyryabov.order.entity.OrderEntity;
import com.github.arseniyryabov.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        OrderEntity order = orderService.createOrder(request, userId);
        OrderResponse response = mapToResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<OrderResponse> getUserOrders(
            @RequestHeader("X-User-Id") Long userId) {
        List<OrderEntity> orders = orderService.getUserOrders(userId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrderById(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") Long userId) {
        OrderEntity order = orderService.getOrderById(orderId, userId);
        return mapToResponse(order);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam String status) {
        OrderEntity order = orderService.updateOrderStatus(orderId, status);
        return mapToResponse(order);
    }

    @GetMapping("/status/{status}")
    public List<OrderResponse> getOrdersByStatus(
            @PathVariable String status) {
        List<OrderEntity> orders = orderService.getOrdersByStatus(status);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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
