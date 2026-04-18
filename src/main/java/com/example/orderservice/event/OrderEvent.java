package com.example.orderservice.event;

import com.example.orderservice.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        String eventType,
        UUID orderId,
        OrderStatus status,
        Instant timestamp
) {
    public static OrderEvent created(UUID orderId, OrderStatus status) {
        return new OrderEvent("ORDER_CREATED", orderId, status, Instant.now());
    }

    public static OrderEvent updated(UUID orderId, OrderStatus status) {
        return new OrderEvent("ORDER_UPDATED", orderId, status, Instant.now());
    }

    public static OrderEvent deleted(UUID orderId, OrderStatus status) {
        return new OrderEvent("ORDER_DELETED", orderId, status, Instant.now());
    }
}
