package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.event.OrderEvent;
import com.example.orderservice.event.OrderEventPublisher;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository repository;
    @Mock OrderEventPublisher publisher;
    @InjectMocks OrderService service;

    private Order sampleOrder(UUID id) {
        Order o = new Order("cust-1", OrderStatus.PENDING, BigDecimal.TEN);
        // Reflectively set id for testing
        try {
            var field = Order.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(o, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        return o;
    }

    @Test
    void createSavesAndPublishesCreatedEvent() {
        UUID id = UUID.randomUUID();
        Order order = sampleOrder(id);
        when(repository.save(any())).thenReturn(order);

        OrderResponse resp = service.create(new OrderRequest("cust-1", OrderStatus.PENDING, BigDecimal.TEN));

        assertThat(resp.id()).isEqualTo(id);
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_CREATED");
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findAllReturnsMappedResponses() {
        UUID id = UUID.randomUUID();
        when(repository.findAll()).thenReturn(List.of(sampleOrder(id)));
        List<OrderResponse> all = service.findAll();
        assertThat(all).hasSize(1).first().extracting(OrderResponse::id).isEqualTo(id);
    }

    @Test
    void updateSavesAndPublishesUpdatedEvent() {
        UUID id = UUID.randomUUID();
        Order order = sampleOrder(id);
        when(repository.findById(id)).thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        service.update(id, new OrderRequest("cust-1", OrderStatus.CONFIRMED, BigDecimal.ONE));

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_UPDATED");
    }

    @Test
    void deleteSavesAndPublishesDeletedEvent() {
        UUID id = UUID.randomUUID();
        Order order = sampleOrder(id);
        when(repository.findById(id)).thenReturn(Optional.of(order));

        service.delete(id);

        verify(repository).delete(order);
        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_DELETED");
    }

    @Test
    void updateThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(id, new OrderRequest("c", OrderStatus.PENDING, BigDecimal.ONE)))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(OrderNotFoundException.class);
    }
}
