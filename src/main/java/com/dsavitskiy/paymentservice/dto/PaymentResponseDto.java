package com.dsavitskiy.paymentservice.dto;

import com.dsavitskiy.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponseDto(
        UUID id,
        UUID userId,
        Long orderId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount) {

}