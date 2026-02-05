package com.github.arseniyryabov.order.service;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.entity.OrderEntity;
import com.github.arseniyryabov.order.enums.OrderStatus;
import com.github.arseniyryabov.order.exception.InsufficientStockException;
import com.github.arseniyryabov.order.exception.ProductNotFoundException;
import com.github.arseniyryabov.order.entity.OrderItemEntity;
import com.github.arseniyryabov.order.exception.UserNotFoundException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Сервис для работы с заказами
@Service
@RequiredArgsConstructor
public class  OrderService {

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
        // Проверка существования пользователя
        try {
            userServiceClient.getUserById(userId);
        } catch (UserNotFoundException e) {
            throw e;  // Пробрасывается исключение дальше для обработки в @RestControllerAdvice
        }

        return orderRepository.findByOrderIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
    }

    // Обновление статуса заказа с использованием enum
    @Transactional
    public OrderEntity updateOrderStatus(UUID orderId, String statusString) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Конвертируем строку в enum
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.fromCode(statusString);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Неверный статус заказа: " + statusString);
        }

        // Если заказ отменен, то товары вернутся на склад
        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            returnProductsToStock(order);
        }

        // Проверяем, возможен ли переход к новому статусу
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // Валидация перехода статусов
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Пример простой валидации
        if (currentStatus.isFinal()) {
            throw new IllegalStateException(
                    "Заказ в статусе " + currentStatus + " не может быть изменен"
            );
        }

        if (!newStatus.canBeCancelled() && newStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Заказ в статусе " + currentStatus + " не может быть отменен"
            );
        }
    }

    // Получение заказов по статусу с использованием enum
    public List<OrderEntity> getOrdersByStatus(String statusString) {
        OrderStatus status;
        try {
            status = OrderStatus.fromCode(statusString);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Неверный статус заказа: " + statusString);
        }

        return orderRepository.findByStatus(status);
    }

    // Метод для получения всех возможных статусов
    public Map<String, String> getAllStatuses() {
        return Arrays.stream(OrderStatus.values())
                .collect(Collectors.toMap(
                        OrderStatus::getCode,
                        OrderStatus::getDescription
                ));
    }

    // Проверка существования пользователя с выбрасыванием исключения
    private void validateUserExistenceOrThrow(Long userId) {
        userServiceClient.getUserById(userId);
    }

    /* Проверка наличия всех товаров в заказе
    Проверка наличия товара через product-service
    Получение информации о товаре для детализации ошибки*/
    private void validateProductsAvailability(List<CreateOrderRequest.OrderItemRequest> items) {
        for (CreateOrderRequest.OrderItemRequest itemRequest : items) {
            UUID productId = itemRequest.getProductId();
            Integer requestedQuantity = itemRequest.getQuantity();

            try {
                // Проверяем доступность товара
                boolean isAvailable = productServiceClient.isProductAvailable(productId, requestedQuantity);
                if (!isAvailable) {
                    // Если товар существует, но недостаточно на складе
                    ProductResponse product = productServiceClient.getProductById(productId);
                    throw new InsufficientStockException(
                            productId,
                            product.getStockQuantity(),
                            requestedQuantity
                    );
                }
            } catch (ProductNotFoundException e) {
                // Если товар не найден, пробрасываем исключение дальше
                throw e;
            }
        }
    }

    // Расчет общей суммы заказа
    private BigDecimal calculateTotalAmount(List<CreateOrderRequest.OrderItemRequest> items) {
        return items.stream()
                .map(itemRequest -> {
                    // Если товар не найден, getProductById выбросит ProductNotFoundException
                    ProductResponse product = productServiceClient.getProductById(itemRequest.getProductId());
                    return product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    /* Уменьшение количества товаров на складе
    catch: Если не удалось уменьшить количество, отменяем весь заказ */
    private void decreaseProductsStock(List<CreateOrderRequest.OrderItemRequest> items) {
        items.forEach(itemRequest -> {
            productServiceClient.decreaseProductStock(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );
        });
    }

    /* Возврат товаров на склад при отмене заказа
    try: Логирование, временная реализация
    - Для продакта добавить endpoint в ProductService.
    - productServiceClient.increaseProductStock(item.getProductId(), item.getQuantity());
    catch: Логирование ошибки, но отмена заказа не прерывается */
    private void returnProductsToStock(OrderEntity order) {
        order.getItems()
                .forEach(item -> {
                    System.out.println("Возврат товара на склад: " +
                            item.getProductId() + ", количество: " + item.getQuantity());
                });
    }
}