package com.dsavitskiy.paymentservice.dto;

import com.dsavitskiy.paymentservice.entity.PaymentStatus;

import java.util.UUID;

public record PaymentSearchDto(
        UUID userId,
        Long orderId,
        PaymentStatus status) {

}