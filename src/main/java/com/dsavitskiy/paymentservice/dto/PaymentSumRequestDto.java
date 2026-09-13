package com.dsavitskiy.paymentservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record PaymentSumRequestDto(
        @NotNull(message = "Start date is obligatory")
        Instant startDate,
        @NotNull(message = "End date is obligatory")
        Instant endDate) {

}