package com.dsavitskiy.paymentservice.controller;

import com.dsavitskiy.paymentservice.dto.*;
import com.dsavitskiy.paymentservice.security.CurrentUser;
import com.dsavitskiy.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody PaymentRequestDto dto) {
        PaymentResponseDto response = paymentService.createPayment(currentUser.getUserId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByUser(
            @PathVariable UUID userId,
            @Valid @ModelAttribute PaymentSearchDto filter) {
        List<PaymentResponseDto> payments = paymentService.getPaymentsByFilter(userId, currentUser.getUserId(), filter);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> searchAllPayments(
            @Valid @ModelAttribute PaymentSearchDto filter) {

        List<PaymentResponseDto> payments = paymentService.searchAllPayments(filter);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}/total")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentSumResponseDto> getUserTotalAmount(
            @PathVariable UUID userId,
            @Valid @ModelAttribute PaymentSumRequestDto request) {
        PaymentSumResponseDto response = paymentService.getUserTotalAmount(
                userId, currentUser.getUserId(), request.startDate(), request.endDate());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentSumResponseDto> getAllTotalAmount(
            @Valid @ModelAttribute PaymentSumRequestDto request) {
        PaymentSumResponseDto response = paymentService.getAllTotalAmount(request.startDate(), request.endDate());
        return ResponseEntity.ok(response);
    }
}