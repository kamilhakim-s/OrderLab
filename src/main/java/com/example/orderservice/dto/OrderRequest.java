package com.example.orderservice.dto;

import com.example.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank String customerId,
        @NotNull OrderStatus status,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount
) {}
