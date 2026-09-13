package com.dsavitskiy.paymentservice.service;

import com.dsavitskiy.paymentservice.client.RandomNumberClient;
import com.dsavitskiy.paymentservice.dto.PaymentRequestDto;
import com.dsavitskiy.paymentservice.dto.PaymentResponseDto;
import com.dsavitskiy.paymentservice.dto.PaymentSearchDto;
import com.dsavitskiy.paymentservice.dto.PaymentSumResponseDto;
import com.dsavitskiy.paymentservice.entity.Payment;
import com.dsavitskiy.paymentservice.entity.PaymentStatus;
import com.dsavitskiy.paymentservice.event.PaymentCreatedEvent;
import com.dsavitskiy.paymentservice.exception.ForbiddenAccessException;
import com.dsavitskiy.paymentservice.kafka.PaymentEventProducer;
import com.dsavitskiy.paymentservice.mapper.PaymentMapper;
import com.dsavitskiy.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentMapper mapper;
    private final RandomNumberClient randomNumberClient;

    private final PaymentEventProducer paymentEventProducer;

    public PaymentResponseDto createPayment(UUID userId, PaymentRequestDto dto) {
        log.info("Creating payment for user: {}", userId);

        Payment payment = mapper.toEntity(dto);
        payment.setUserId(userId);

        int randomNum = randomNumberClient.getRandomNumber();
        log.debug("Getting random number: {}", randomNum);

        payment.setStatus(randomNum % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        Payment savedPayment = repository.save(payment);
        log.info("Payment saved with ID: {} and status: {}", savedPayment.getId(), savedPayment.getStatus());

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                savedPayment.getId(),
                savedPayment.getUserId(),
                savedPayment.getOrderId(),
                savedPayment.getPaymentAmount(),
                savedPayment.getStatus(),
                savedPayment.getTimestamp()
        );
        paymentEventProducer.sendPaymentCreatedEvent(event, savedPayment.getOrderId());

        return mapper.toDto(savedPayment);
    }

    public List<PaymentResponseDto> getPaymentsByFilter(UUID requestedUserId, UUID currentUserId, PaymentSearchDto filter) {
        log.info("Searching payments. Requested user: {}, Current user: {}", requestedUserId, currentUserId);
        if (!requestedUserId.equals(currentUserId)) {
            throw new ForbiddenAccessException("Access denied");
        }

        PaymentSearchDto secureFilter = new PaymentSearchDto(requestedUserId, filter.orderId(), filter.status());
        List<Payment> payments = repository.searchPayments(secureFilter);
        return mapper.toListDto(payments);
    }

    public List<PaymentResponseDto> searchAllPayments(PaymentSearchDto filter) {
        log.info("Searching payments for all users: {}", filter);
        List<Payment> payments = repository.searchPayments(filter);
        return mapper.toListDto(payments);
    }

    public PaymentSumResponseDto getUserTotalAmount(UUID requestedUserId, UUID currentUserId, Instant startDate, Instant endDate) {
        log.info("Count sum for user {}. Current user: {}", requestedUserId, currentUserId);

        if (!requestedUserId.equals(currentUserId)) {
            throw new ForbiddenAccessException("Access denied");
        }

        PaymentSumResponseDto result = repository.getTotalAmountByUserAndDateRange(requestedUserId, startDate, endDate);
        return result != null ? result : new PaymentSumResponseDto(BigDecimal.ZERO, 0);
    }

    public PaymentSumResponseDto getAllTotalAmount(Instant startDate, Instant endDate) {
        log.info("Count sum for all users per period [{} - {}]", startDate, endDate);
        PaymentSumResponseDto result = repository.getTotalAmountByDateRange(startDate, endDate);
        return result != null ? result : new PaymentSumResponseDto(BigDecimal.ZERO, 0);
    }
}