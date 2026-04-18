package com.example.orderservice.event;

import com.example.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderEventPublisher publisher;
    private static final String EXCHANGE = "orders.exchange";

    @BeforeEach
    void setUp() {
        publisher = new OrderEventPublisher(rabbitTemplate, EXCHANGE);
    }

    @Test
    void publishesCreatedEventWithCorrectRoutingKey() {
        UUID id = UUID.randomUUID();
        OrderEvent event = OrderEvent.created(id, OrderStatus.PENDING);

        publisher.publish(event);

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(EXCHANGE),
                routingKeyCaptor.capture(),
                payloadCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("order.created");
        assertThat(((OrderEvent) payloadCaptor.getValue()).orderId()).isEqualTo(id);
        assertThat(((OrderEvent) payloadCaptor.getValue()).eventType()).isEqualTo("ORDER_CREATED");
    }

    @Test
    void publishesUpdatedEventWithCorrectRoutingKey() {
        UUID id = UUID.randomUUID();
        publisher.publish(OrderEvent.updated(id, OrderStatus.CONFIRMED));

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(EXCHANGE),
                routingKeyCaptor.capture(),
                payloadCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("order.updated");
    }

    @Test
    void publishesDeletedEventWithCorrectRoutingKey() {
        UUID id = UUID.randomUUID();
        publisher.publish(OrderEvent.deleted(id, OrderStatus.CANCELLED));

        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(EXCHANGE),
                routingKeyCaptor.capture(),
                payloadCaptor.capture()
        );
        assertThat(routingKeyCaptor.getValue()).isEqualTo("order.deleted");
    }
}
