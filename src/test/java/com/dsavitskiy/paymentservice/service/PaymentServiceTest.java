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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class  PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private PaymentMapper mapper;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userId;
    private Long orderId;
    private PaymentRequestDto requestDto;
    private Payment payment;
    private PaymentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = 1L;

        requestDto = new PaymentRequestDto(orderId, new BigDecimal("100.50"));

        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setPaymentAmount(new BigDecimal("100.50"));
        payment.setTimestamp(Instant.now());

        responseDto = new PaymentResponseDto(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getTimestamp(),
                payment.getPaymentAmount()
        );
    }



    @Test
    void createPayment_WhenRandomNumberIsEven_ShouldSetSuccessStatus() {
        when(randomNumberClient.getRandomNumber()).thenReturn(2);
        when(mapper.toEntity(any(PaymentRequestDto.class))).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toDto(any(Payment.class))).thenReturn(responseDto);

        PaymentResponseDto result = paymentService.createPayment(userId, requestDto);

        assertThat(result).isNotNull();

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(paymentEventProducer).sendPaymentCreatedEvent(any(PaymentCreatedEvent.class), eq(orderId));
    }

    @Test
    void createPayment_WhenRandomNumberIsOdd_ShouldSetFailedStatus() {
        when(randomNumberClient.getRandomNumber()).thenReturn(3);
        when(mapper.toEntity(any(PaymentRequestDto.class))).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toDto(any(Payment.class))).thenReturn(responseDto);

        paymentService.createPayment(userId, requestDto);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void createPayment_ShouldSendKafkaEvent() {
        when(randomNumberClient.getRandomNumber()).thenReturn(4);
        when(mapper.toEntity(any(PaymentRequestDto.class))).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toDto(any(Payment.class))).thenReturn(responseDto);

        paymentService.createPayment(userId, requestDto);

        ArgumentCaptor<PaymentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);
        verify(paymentEventProducer).sendPaymentCreatedEvent(eventCaptor.capture(), eq(orderId));

        PaymentCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.paymentId()).isEqualTo(payment.getId());
        assertThat(capturedEvent.userId()).isEqualTo(userId);
        assertThat(capturedEvent.orderId()).isEqualTo(orderId);
        assertThat(capturedEvent.paymentAmount()).isEqualTo(new BigDecimal("100.50"));
    }

    @Test
    void createPayment_ShouldSetUserIdInPayment() {
        when(randomNumberClient.getRandomNumber()).thenReturn(2);
        when(mapper.toEntity(any(PaymentRequestDto.class))).thenReturn(payment);
        when(repository.save(any(Payment.class))).thenReturn(payment);
        when(mapper.toDto(any(Payment.class))).thenReturn(responseDto);

        paymentService.createPayment(userId, requestDto);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getUserId()).isEqualTo(userId);
    }


    @Test
    void getPaymentsByFilter_WhenUserHasAccess_ShouldReturnPayments() {
        PaymentSearchDto filter = new PaymentSearchDto(userId, orderId, PaymentStatus.SUCCESS);
        List<Payment> payments = List.of(payment);

        when(repository.searchPayments(any(PaymentSearchDto.class))).thenReturn(payments);
        when(mapper.toListDto(payments)).thenReturn(List.of(responseDto));

        List<PaymentResponseDto> result = paymentService.getPaymentsByFilter(userId, userId, filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderId()).isEqualTo(orderId);
        verify(repository).searchPayments(any(PaymentSearchDto.class));
    }

    @Test
    void getPaymentsByFilter_WhenUsersDontMatch_ShouldThrowException() {
        UUID requestedUserId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        PaymentSearchDto filter = new PaymentSearchDto(requestedUserId, orderId, null);

        assertThatThrownBy(() -> paymentService.getPaymentsByFilter(requestedUserId, currentUserId, filter))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("Access denied");

        verify(repository, never()).searchPayments(any());
    }

    @Test
    void getPaymentsByFilter_WhenNoPaymentsFound_ShouldReturnEmptyList() {
        PaymentSearchDto filter = new PaymentSearchDto(userId, orderId, null);
        when(repository.searchPayments(any(PaymentSearchDto.class))).thenReturn(Collections.emptyList());
        when(mapper.toListDto(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<PaymentResponseDto> result = paymentService.getPaymentsByFilter(userId, userId, filter);

        assertThat(result).isEmpty();
    }

    @Test
    void getPaymentsByFilter_ShouldCreateSecureFilter() {
        PaymentSearchDto filter = new PaymentSearchDto(userId, orderId, PaymentStatus.SUCCESS);
        when(repository.searchPayments(any(PaymentSearchDto.class))).thenReturn(Collections.emptyList());
        when(mapper.toListDto(any())).thenReturn(Collections.emptyList());

        paymentService.getPaymentsByFilter(userId, userId, filter);

        ArgumentCaptor<PaymentSearchDto> filterCaptor = ArgumentCaptor.forClass(PaymentSearchDto.class);
        verify(repository).searchPayments(filterCaptor.capture());

        PaymentSearchDto capturedFilter = filterCaptor.getValue();
        assertThat(capturedFilter.userId()).isEqualTo(userId);
    }


    @Test
    void searchAllPayments_ShouldReturnAllPayments() {
        PaymentSearchDto filter = new PaymentSearchDto(null, orderId, PaymentStatus.SUCCESS);
        List<Payment> payments = List.of(payment);

        when(repository.searchPayments(filter)).thenReturn(payments);
        when(mapper.toListDto(payments)).thenReturn(List.of(responseDto));

        List<PaymentResponseDto> result = paymentService.searchAllPayments(filter);

        assertThat(result).hasSize(1);
        verify(repository).searchPayments(filter);
    }

    @Test
    void searchAllPayments_WhenNoPaymentsFound_ShouldReturnEmptyList() {
        PaymentSearchDto filter = new PaymentSearchDto(null, null, null);
        when(repository.searchPayments(filter)).thenReturn(Collections.emptyList());
        when(mapper.toListDto(Collections.emptyList())).thenReturn(Collections.emptyList());

        List<PaymentResponseDto> result = paymentService.searchAllPayments(filter);

        assertThat(result).isEmpty();
    }


    @Test
    void getUserTotalAmount_WhenUserHasAccess_ShouldReturnTotalAmount() {
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();
        PaymentSumResponseDto expectedResult = new PaymentSumResponseDto(new BigDecimal("500.00"), 5);

        when(repository.getTotalAmountByUserAndDateRange(userId, startDate, endDate))
                .thenReturn(expectedResult);

        PaymentSumResponseDto result = paymentService.getUserTotalAmount(userId, userId, startDate, endDate);

        assertThat(result.totalAmount()).isEqualTo(new BigDecimal("500.00"));
        assertThat(result.paymentsCount()).isEqualTo(5);
        verify(repository).getTotalAmountByUserAndDateRange(userId, startDate, endDate);
    }

    @Test
    void getUserTotalAmount_WhenUsersDontMatch_ShouldThrowException() {
        UUID requestedUserId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        assertThatThrownBy(() ->
                paymentService.getUserTotalAmount(requestedUserId, currentUserId, startDate, endDate))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("Access denied");

        verify(repository, never()).getTotalAmountByUserAndDateRange(any(), any(), any());
    }

    @Test
    void getUserTotalAmount_WhenNoPaymentsFound_ShouldReturnZero() {
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        when(repository.getTotalAmountByUserAndDateRange(userId, startDate, endDate))
                .thenReturn(null);

        PaymentSumResponseDto result = paymentService.getUserTotalAmount(userId, userId, startDate, endDate);

        assertThat(result.totalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.paymentsCount()).isEqualTo(0);
    }


    @Test
    void getAllTotalAmount_ShouldReturnTotalAmount() {
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();
        PaymentSumResponseDto expectedResult = new PaymentSumResponseDto(new BigDecimal("1000.00"), 10);

        when(repository.getTotalAmountByDateRange(startDate, endDate))
                .thenReturn(expectedResult);

        PaymentSumResponseDto result = paymentService.getAllTotalAmount(startDate, endDate);

        assertThat(result.totalAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.paymentsCount()).isEqualTo(10);
        verify(repository).getTotalAmountByDateRange(startDate, endDate);
    }

    @Test
    void getAllTotalAmount_WhenNoPaymentsFound_ShouldReturnZero() {
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();

        when(repository.getTotalAmountByDateRange(startDate, endDate))
                .thenReturn(null);

        PaymentSumResponseDto result = paymentService.getAllTotalAmount(startDate, endDate);

        assertThat(result.totalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.paymentsCount()).isEqualTo(0);
    }
}