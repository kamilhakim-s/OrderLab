package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderNotFoundException;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean OrderService service;

    private OrderResponse sampleResponse(UUID id) {
        return new OrderResponse(id, "cust-1", OrderStatus.PENDING, BigDecimal.TEN, Instant.now(), Instant.now());
    }

    @Test
    void postOrderReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.create(any())).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest("cust-1", OrderStatus.PENDING, BigDecimal.TEN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getOrderByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("cust-1"));
    }

    @Test
    void getOrderByIdReturns404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new OrderNotFoundException(id));

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllOrdersReturns200() throws Exception {
        when(service.findAll()).thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void putOrderReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.update(eq(id), any())).thenReturn(sampleResponse(id));

        mockMvc.perform(put("/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest("cust-1", OrderStatus.CONFIRMED, BigDecimal.TEN))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteOrderReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        mockMvc.perform(delete("/orders/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void postOrderReturns400WhenInvalidBody() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
