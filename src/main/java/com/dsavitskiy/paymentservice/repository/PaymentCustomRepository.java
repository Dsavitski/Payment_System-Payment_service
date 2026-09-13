package com.dsavitskiy.paymentservice.repository;

import com.dsavitskiy.paymentservice.dto.PaymentSearchDto;
import com.dsavitskiy.paymentservice.dto.PaymentSumResponseDto;
import com.dsavitskiy.paymentservice.entity.Payment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentCustomRepository {

    List<Payment> searchPayments(PaymentSearchDto paymentSearchDto);

    PaymentSumResponseDto getTotalAmountByUserAndDateRange(UUID userId, Instant startDate, Instant endDate);

    PaymentSumResponseDto getTotalAmountByDateRange(Instant startDate, Instant endDate);
}
