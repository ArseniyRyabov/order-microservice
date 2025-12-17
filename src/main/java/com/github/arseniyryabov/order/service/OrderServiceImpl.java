package com.github.arseniyryabov.order.service;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.entity.OrderEntity;
import com.github.arseniyryabov.order.exception.ProductNotFoundException;
import com.github.arseniyryabov.order.exception.UserNotFoundException;
import com.github.arseniyryabov.order.entity.OrderItemEntity;
import com.github.arseniyryabov.order.integrations.ProductResponse;

import com.github.arseniyryabov.order.integrations.client.ProductServiceClient;
import com.github.arseniyryabov.order.integrations.client.UserServiceClient;
import com.github.arseniyryabov.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    @Override
    @Transactional
    public OrderEntity createOrder(CreateOrderRequest request, Long userId) {
        // 1. Валидация user
        validateUserExists(userId);


        // 2. Валидация продуктов и расчет суммы
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product = productServiceClient.getProductById(itemRequest.getProductId());

            // Проверка наличия продукта
            if (product == null) {
                throw new ProductNotFoundException(itemRequest.getProductId());
            }

            BigDecimal itemPrice = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);
        }

        // 3. Создание заказа
        OrderEntity order = new OrderEntity();
        order.setOrderId(UUID.randomUUID());
        order.setUserId(userId);
        order.setAddress(request.getAddress());
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setStatus("CREATED");
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());

        // 4. Создание элементов заказа
        List<OrderItemEntity> items = request.getItems().stream()
                .map(itemRequest -> {
                    OrderItemEntity item = new OrderItemEntity();
                    item.setOrderItemId(UUID.randomUUID());
                    item.setOrder(order);
                    item.setProductId(itemRequest.getProductId());
                    item.setQuantity(itemRequest.getQuantity());
                    return item;
                })
                .collect(Collectors.toList());

        order.setItems(items);

        // 5. Сохранение заказа
        return orderRepository.save(order);
    }

    @Override
    public List<OrderEntity> getUserOrders(Long userId) {
        validateUserExists(userId);

        return orderRepository.findByUserId(userId);
    }

    @Override
    public OrderEntity getOrderById(UUID orderId, Long userId) {
        OrderEntity order = orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        return order;
    }

    @Override
    @Transactional
    public OrderEntity updateOrderStatus(UUID orderId, String status) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public List<OrderEntity> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    private void validateUserExists(Long userId) {
        if (!userServiceClient.validateUserExists(userId)) {
            throw new UserNotFoundException(userId);
        }
    }
}
