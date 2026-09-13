package com.dsavitskiy.paymentservice.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String description,
        String message) {

}