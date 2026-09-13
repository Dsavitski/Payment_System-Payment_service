package com.dsavitskiy.paymentservice.event;

import com.dsavitskiy.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreatedEvent(
        UUID paymentId,
        UUID userId,
        Long orderId,
        BigDecimal paymentAmount,
        PaymentStatus status,
        Instant timestamp) {

}