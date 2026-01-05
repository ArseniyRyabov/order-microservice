package com.github.arseniyryabov.order.service;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.entity.OrderEntity;
import com.github.arseniyryabov.order.exception.InsufficientStockException;
import com.github.arseniyryabov.order.exception.ProductNotFoundException;
import com.github.arseniyryabov.order.exception.ProductUnavailableException;
import com.github.arseniyryabov.order.entity.OrderItemEntity;
import com.github.arseniyryabov.order.integration.ProductResponse;

import com.github.arseniyryabov.order.integration.client.ProductServiceClient;
import com.github.arseniyryabov.order.integration.client.UserServiceClient;
import com.github.arseniyryabov.order.repository.OrderItemRepository;
import com.github.arseniyryabov.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Сервис для работы с заказами
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    // Создание нового заказа
    @Transactional
    public OrderEntity createOrder(CreateOrderRequest request, Long userId) {

        // Проверка существования пользователя
        validateUserExistenceOrThrow(userId);

        // Проверка наличия всех товаров перед расчетом суммы
        validateProductsAvailability(request.getItems());

        // Расчет общей суммы заказа
        BigDecimal totalAmount = calculateTotalAmount(request.getItems());

        // Создание объекта заказа
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setAddress(request.getAddress());
        order.setDeliveryMethod(request.getDeliveryMethod());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());

        // Сохранение заказа, чтобы получить ID
        order = orderRepository.save(order);

        // Создание элементов заказа
        List<OrderItemEntity> items = createOrderItems(order, request.getItems());
        order.setItems(items);

        // Уменьшение количества товаров на складе
        decreaseProductsStock(request.getItems());

        // Сохранение заказа с элементами
        return orderRepository.save(order);
    }

    // Получение заказов пользователя
    public List<OrderEntity> getUserOrders(Long userId) {

        // Проверка существования пользователя (если не найден, будет 404)
        userServiceClient.getUserById(userId);

        return orderRepository.findByUserId(userId);
    }

    // Получение конкретного заказа пользователя
    public OrderEntity getOrderById(UUID orderId, Long userId) {
        // Проверка существования пользователя (если не найден, будет 404)
        userServiceClient.getUserById(userId);

        return orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    // Обновление статуса заказа
    @Transactional
    public OrderEntity updateOrderStatus(UUID orderId, String status) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Если заказ отменен, то товары вернутся на склад
        if (OrderStatus.CANCELLED.equals(status) && !OrderStatus.CANCELLED.equals(order.getStatus())) {
            returnProductsToStock(order);
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }

    // Получение заказов по статусу
    public List<OrderEntity> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    // Проверка существования пользователя с выбрасыванием исключения
    private void validateUserExistenceOrThrow(Long userId) {
        userServiceClient.getUserById(userId);
    }

    // Проверка наличия всех товаров в заказе
    private void validateProductsAvailability(List<CreateOrderRequest.OrderItemRequest> items) {
        for (CreateOrderRequest.OrderItemRequest itemRequest : items) {
            UUID productId = itemRequest.getProductId();
            Integer requestedQuantity = itemRequest.getQuantity();

            // Проверяем наличие товара через product-service
            boolean isAvailable = productServiceClient.isProductAvailable(productId, requestedQuantity);

            if (!isAvailable) {

                // Получение информации о товаре для детализации ошибки
                ProductResponse product = productServiceClient.getProductById(productId);
                if (product == null) {
                    throw new ProductNotFoundException(productId);
                }

                if (product.getStockQuantity() == 0) {
                    throw new ProductUnavailableException(productId);
                } else {
                    throw new InsufficientStockException(
                            productId,
                            product.getStockQuantity(),
                            requestedQuantity
                    );
                }
            }
        }
    }

    // Расчет общей суммы заказа
    private BigDecimal calculateTotalAmount(List<CreateOrderRequest.OrderItemRequest> items) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : items) {
            ProductResponse product = productServiceClient.getProductById(itemRequest.getProductId());

            if (product == null) {
                throw new ProductNotFoundException(itemRequest.getProductId());
            }

            BigDecimal itemPrice = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemPrice);
        }

        return totalAmount;
    }

    // Создание элементов заказа
    private List<OrderItemEntity> createOrderItems(OrderEntity order,
                                                   List<CreateOrderRequest.OrderItemRequest> items) {
        return items.stream()
                .map(itemRequest -> {
                    OrderItemEntity item = new OrderItemEntity();
                    item.setOrder(order);
                    item.setProductId(itemRequest.getProductId());
                    item.setQuantity(itemRequest.getQuantity());
                    return item;
                })
                .collect(Collectors.toList());
    }

    // Уменьшение количества товаров на складе
    private void decreaseProductsStock(List<CreateOrderRequest.OrderItemRequest> items) {
        for (CreateOrderRequest.OrderItemRequest itemRequest : items) {
            try {
                productServiceClient.decreaseProductStock(
                        itemRequest.getProductId(),
                        itemRequest.getQuantity()
                );
            } catch (Exception e) {
                // Если не удалось уменьшить количество, отменяем весь заказ
                throw new RuntimeException("Не удалось обновить количество товара на складе: " + e.getMessage());
            }
        }
    }

    // Возврат товаров на склад при отмене заказа
    private void returnProductsToStock(OrderEntity order) {
        List<OrderItemEntity> items = order.getItems();

        for (OrderItemEntity item : items) {
            try {
                // Для продакта добавить endpoint в ProductService
                // productServiceClient.increaseProductStock(item.getProductId(), item.getQuantity());

                // Логирование, временная реализация
                System.out.println("Возврат товара на склад: " +
                        item.getProductId() + ", количество: " + item.getQuantity());
            } catch (Exception e) {
                // Логирование ошибки, но отмена заказа не прерывается
                System.err.println("Ошибка при возврате товара на складе: " + e.getMessage());
            }
        }
    }

    // Класс со статусами заказа
    public static class OrderStatus {
        public static final String CREATED = "CREATED";      // Создан
        public static final String PROCESSING = "PROCESSING"; // В обработке
        public static final String SHIPPED = "SHIPPED";      // Отправлен
        public static final String DELIVERED = "DELIVERED";   // Доставлен
        public static final String CANCELLED = "CANCELLED";   // Отменен
        public static final String PENDING = "PENDING";       // В ожидании
    }
}
