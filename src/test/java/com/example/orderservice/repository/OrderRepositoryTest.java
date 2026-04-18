package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    void savesAndFindsById() {
        Order saved = repository.save(new Order("cust-1", OrderStatus.PENDING, BigDecimal.TEN));
        Optional<Order> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo("cust-1");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void findByCustomerIdReturnsMatchingOrders() {
        repository.save(new Order("cust-A", OrderStatus.PENDING, BigDecimal.ONE));
        repository.save(new Order("cust-A", OrderStatus.CONFIRMED, BigDecimal.TEN));
        repository.save(new Order("cust-B", OrderStatus.PENDING, BigDecimal.ONE));

        List<Order> results = repository.findByCustomerId("cust-A");
        assertThat(results).hasSize(2)
                .allMatch(o -> "cust-A".equals(o.getCustomerId()));
    }

    @Test
    void findByCustomerIdReturnsEmptyWhenNoneMatch() {
        assertThat(repository.findByCustomerId("unknown")).isEmpty();
    }

    @Test
    void timestampsArePopulatedOnSave() {
        Order saved = repository.save(new Order("cust-1", OrderStatus.PENDING, BigDecimal.TEN));
        em.flush();
        em.refresh(saved);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
