package com.github.arseniyryabov.order_microservice.service;

import com.github.arseniyryabov.order_microservice.dto.request.CreateOrderRequest;
import com.github.arseniyryabov.order_microservice.dto.response.OrderResponse;
import com.github.arseniyryabov.order_microservice.dto.response.OrderItemResponse;
import com.github.arseniyryabov.order_microservice.entity.OrderEntity;
import com.github.arseniyryabov.order_microservice.entity.OrderItemEntity;
import com.github.arseniyryabov.order_microservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setAddress(request.getAddress());
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setTotalAmount(request.getTotalAmount());

        List<OrderItemEntity> items = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItemEntity item = new OrderItemEntity();
                    item.setOrder(order);
                    item.setProductId(UUID.fromString(itemRequest.getProductId()));
                    item.setQuantity(itemRequest.getQuantity());
                    return item;
                })
                .collect(Collectors.toList());

        order.setItems(items);

        OrderEntity savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    public List<OrderResponse> getUserOrders(Long userId) {
        List<OrderEntity> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(UUID orderId, Long userId) {
        OrderEntity order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        order.setStatus(status);
        OrderEntity updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    public List<OrderResponse> getOrdersByStatus(String status) {
        List<OrderEntity> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteOrder(UUID orderId, Long userId) {
        OrderEntity order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        orderRepository.delete(order);
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

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> {
                    OrderItemResponse itemResponse = new OrderItemResponse();
                    itemResponse.setOrderItemId(item.getOrderItemId());
                    itemResponse.setProductId(item.getProductId());
                    itemResponse.setQuantity(item.getQuantity());
                    return itemResponse;
                })
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }
}
