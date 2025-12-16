package com.github.arseniyryabov.order_microservice.repository;

import com.github.arseniyryabov.order_microservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    List<OrderEntity> findByUserId(Long userId);

    Optional<OrderEntity> findByOrderIdAndUserId(UUID orderId, Long userId);

    List<OrderEntity> findByStatus(String status);
}
