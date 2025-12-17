package com.github.arseniyryabov.order.service;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.entity.OrderEntity;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderEntity createOrder(CreateOrderRequest request, Long userId);
    List<OrderEntity> getUserOrders(Long userId);
    OrderEntity getOrderById(UUID orderId, Long userId);
    OrderEntity updateOrderStatus(UUID orderId, String status);
    List<OrderEntity> getOrdersByStatus(String status);
}
