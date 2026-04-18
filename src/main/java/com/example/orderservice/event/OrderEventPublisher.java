package com.example.orderservice.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate,
                               @Value("${app.rabbitmq.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    public void publish(OrderEvent event) {
        String routingKey = switch (event.eventType()) {
            case "ORDER_CREATED" -> "order.created";
            case "ORDER_UPDATED" -> "order.updated";
            case "ORDER_DELETED" -> "order.deleted";
            default -> throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        };
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
