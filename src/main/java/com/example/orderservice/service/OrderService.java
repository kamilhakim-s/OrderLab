package com.example.orderservice.service;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.event.OrderEvent;
import com.example.orderservice.event.OrderEventPublisher;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repository, OrderEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public OrderResponse create(OrderRequest req) {
        Order order = repository.save(new Order(req.customerId(), req.status(), req.totalAmount()));
        publisher.publish(OrderEvent.created(order.getId(), order.getStatus()));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return OrderResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll().stream().map(OrderResponse::from).toList();
    }

    public OrderResponse update(UUID id, OrderRequest req) {
        Order order = getOrThrow(id);
        order.setCustomerId(req.customerId());
        order.setStatus(req.status());
        order.setTotalAmount(req.totalAmount());
        Order saved = repository.save(order);
        publisher.publish(OrderEvent.updated(saved.getId(), saved.getStatus()));
        return OrderResponse.from(saved);
    }

    public void delete(UUID id) {
        Order order = getOrThrow(id);
        repository.delete(order);
        publisher.publish(OrderEvent.deleted(order.getId(), order.getStatus()));
    }

    private Order getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
