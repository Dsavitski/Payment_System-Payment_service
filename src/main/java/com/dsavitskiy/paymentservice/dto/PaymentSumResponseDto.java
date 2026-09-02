package com.dsavitskiy.paymentservice.dto;

import java.math.BigDecimal;

public record PaymentSumResponseDto(
        BigDecimal totalAmount,
        Integer paymentsCount) {

}