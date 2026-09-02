package com.dsavitskiy.paymentservice.mapper;

import com.dsavitskiy.paymentservice.dto.PaymentRequestDto;
import com.dsavitskiy.paymentservice.dto.PaymentResponseDto;
import com.dsavitskiy.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponseDto toDto(Payment payment);

    List<PaymentResponseDto> toListDto(List<Payment> payments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status" , constant = "PENDING")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
    Payment toEntity(PaymentRequestDto paymentRequestDto);
}
