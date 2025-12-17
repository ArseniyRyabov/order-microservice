package com.github.arseniyryabov.order.service;

import static org.junit.jupiter.api.Assertions.*;

import com.github.arseniyryabov.order.controller.model.CreateOrderRequest;
import com.github.arseniyryabov.order.entity.OrderEntity;
import com.github.arseniyryabov.order.entity.OrderItemEntity;
import com.github.arseniyryabov.order.integrations.client.ProductServiceClient;
import com.github.arseniyryabov.order.integrations.client.UserServiceClient;
import com.github.arseniyryabov.order.repository.OrderRepository;
import com.github.arseniyryabov.order.exception.ProductNotFoundException;
import com.github.arseniyryabov.order.exception.UserNotFoundException;
import com.github.arseniyryabov.order.integrations.ProductResponse;
import com.github.arseniyryabov.order.integrations.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Long userId;
    private UUID productId1;
    private UUID productId2;

    @BeforeEach
    void setUp() {
        userId = 1L;
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();
    }

    @Test
    void createOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddress("Test Address");
        request.setDeliveryMethod("COURIER");

        CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest();
        item1.setProductId(productId1);
        item1.setQuantity(2);

        CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest();
        item2.setProductId(productId2);
        item2.setQuantity(1);

        request.setItems(Arrays.asList(item1, item2));

        UserResponse userResponse = new UserResponse(userId, "john.doe", "Doe", "John");
        ProductResponse product1 = new ProductResponse(productId1, "Product 1",
                new BigDecimal("50.00"), "Desc 1", null, null);
        ProductResponse product2 = new ProductResponse(productId2, "Product 2",
                new BigDecimal("75.00"), "Desc 2", null, null);

        when(userServiceClient.validateUserExists(userId)).thenReturn(true);
        when(productServiceClient.getProductById(productId1)).thenReturn(product1);
        when(productServiceClient.getProductById(productId2)).thenReturn(product2);

        OrderEntity savedOrder = new OrderEntity();
        savedOrder.setOrderId(UUID.randomUUID());
        savedOrder.setUserId(userId);
        savedOrder.setTotalAmount(new BigDecimal("175.00")); // (50*2) + (75*1) = 175

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        OrderEntity result = orderService.createOrder(request, userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(new BigDecimal("175.00"), result.getTotalAmount());

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(productServiceClient, times(1)).getProductById(productId1);
        verify(productServiceClient, times(1)).getProductById(productId2);
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
    }

    @Test
    void createOrder_UserNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Arrays.asList(new CreateOrderRequest.OrderItemRequest()));

        when(userServiceClient.validateUserExists(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> orderService.createOrder(request, userId));

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(productServiceClient, never()).getProductById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_ProductNotFound() {
        CreateOrderRequest request = new CreateOrderRequest();

        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setProductId(productId1);
        item.setQuantity(1);
        request.setItems(Arrays.asList(item));

        when(userServiceClient.validateUserExists(userId)).thenReturn(true);
        when(productServiceClient.getProductById(productId1)).thenReturn(null);

        assertThrows(ProductNotFoundException.class,
                () -> orderService.createOrder(request, userId));

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(productServiceClient, times(1)).getProductById(productId1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getUserOrders_Success() {
        List<OrderEntity> expectedOrders = Arrays.asList(
                createTestOrder(),
                createTestOrder()
        );

        when(userServiceClient.validateUserExists(userId)).thenReturn(true);
        when(orderRepository.findByUserId(userId)).thenReturn(expectedOrders);

        List<OrderEntity> result = orderService.getUserOrders(userId);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(orderRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getUserOrders_UserNotFound() {
        when(userServiceClient.validateUserExists(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class,
                () -> orderService.getUserOrders(userId));

        verify(userServiceClient, times(1)).validateUserExists(userId);
        verify(orderRepository, never()).findByUserId(any());
    }

    @Test
    void getOrderById_Success() {
        UUID orderId = UUID.randomUUID();
        OrderEntity expectedOrder = createTestOrder();
        expectedOrder.setOrderId(orderId); // Устанавливаем orderId

        when(orderRepository.findByOrderIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(expectedOrder));

        OrderEntity result = orderService.getOrderById(orderId, userId);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());

        verify(orderRepository, times(1)).findByOrderIdAndUserId(orderId, userId);
        verify(userServiceClient, never()).validateUserExists(any()); // Этот вызов не должен происходить
    }

    @Test
    void getOrderById_OrderNotFound() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByOrderIdAndUserId(orderId, userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.getOrderById(orderId, userId));

        assertTrue(exception.getMessage().contains("Заказ не найден"));

        verify(orderRepository, times(1)).findByOrderIdAndUserId(orderId, userId);
        verify(userServiceClient, never()).validateUserExists(any());
    }

    @Test
    void updateOrderStatus_Success() {
        UUID orderId = UUID.randomUUID();
        String newStatus = "SHIPPED";
        OrderEntity existingOrder = createTestOrder();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderEntity result = orderService.updateOrderStatus(orderId, newStatus);

        assertNotNull(result);
        assertEquals(newStatus, result.getStatus());

        verify(orderRepository, times(1)).findByOrderId(orderId);
        verify(orderRepository, times(1)).save(existingOrder);
        verify(userServiceClient, never()).validateUserExists(any());
        verify(productServiceClient, never()).getProductById(any());
    }

    @Test
    void getOrdersByStatus_Success() {
        String status = "CREATED";
        List<OrderEntity> expectedOrders = Arrays.asList(
                createTestOrder(),
                createTestOrder()
        );

        when(orderRepository.findByStatus(status)).thenReturn(expectedOrders);

        List<OrderEntity> result = orderService.getOrdersByStatus(status);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(orderRepository, times(1)).findByStatus(status);
        verify(userServiceClient, never()).validateUserExists(any());
        verify(productServiceClient, never()).getProductById(any());
    }

    private OrderEntity createTestOrder() {
        OrderEntity order = new OrderEntity();
        order.setOrderId(UUID.randomUUID());
        order.setUserId(userId);
        order.setAddress("Test Address");
        order.setDeliveryMethod("COURIER");
        order.setStatus("CREATED");
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setCreatedAt(LocalDateTime.now());

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderItemId(UUID.randomUUID());
        item.setProductId(productId1);
        item.setQuantity(1);

        order.setItems(Arrays.asList(item));
        return order;
    }
}