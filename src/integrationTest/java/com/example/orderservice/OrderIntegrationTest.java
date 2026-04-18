package com.example.orderservice;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired OrderRepository orderRepository;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitAdmin rabbitAdmin;

    @Value("${app.rabbitmq.queue}") String queue;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
        rabbitAdmin.purgeQueue(queue, false);
    }

    @Test
    void createOrder_returns201_persistsRow_publishesEvent() {
        OrderRequest req = new OrderRequest("cust-1", OrderStatus.PENDING, BigDecimal.TEN);

        ResponseEntity<OrderResponse> resp = restTemplate.postForEntity(
                baseUrl() + "/orders", req, OrderResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        UUID id = resp.getBody().id();

        assertThat(orderRepository.findById(id)).isPresent();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Object msg = rabbitTemplate.receiveAndConvert(queue);
            assertThat(msg).isNotNull();
        });
    }

    @Test
    void updateOrder_changesStatus_publishesEvent() {
        OrderResponse created = restTemplate.postForObject(
                baseUrl() + "/orders",
                new OrderRequest("cust-2", OrderStatus.PENDING, BigDecimal.TEN),
                OrderResponse.class);
        rabbitAdmin.purgeQueue(queue, false);

        HttpEntity<OrderRequest> entity = new HttpEntity<>(
                new OrderRequest("cust-2", OrderStatus.CONFIRMED, BigDecimal.TEN));
        ResponseEntity<OrderResponse> resp = restTemplate.exchange(
                baseUrl() + "/orders/" + created.id(), HttpMethod.PUT, entity, OrderResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo(OrderStatus.CONFIRMED);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(rabbitTemplate.receiveAndConvert(queue)).isNotNull());
    }

    @Test
    void deleteOrder_returns204_orderGone_publishesEvent() {
        OrderResponse created = restTemplate.postForObject(
                baseUrl() + "/orders",
                new OrderRequest("cust-3", OrderStatus.PENDING, BigDecimal.TEN),
                OrderResponse.class);
        rabbitAdmin.purgeQueue(queue, false);

        restTemplate.delete(baseUrl() + "/orders/" + created.id());

        ResponseEntity<String> getResp = restTemplate.getForEntity(
                baseUrl() + "/orders/" + created.id(), String.class);
        assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(rabbitTemplate.receiveAndConvert(queue)).isNotNull());
    }
}
