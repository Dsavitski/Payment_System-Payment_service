package com.dsavitskiy.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequestDto(
        @NotNull(message = "Order id is obligatory")
        Long orderId,
        @NotNull(message = "Payment amount is obligatory")
        @DecimalMin(value = "0.01", message = "Payment amount must be more than 0.00")
        BigDecimal paymentAmount) {

}