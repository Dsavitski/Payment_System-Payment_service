package com.dsavitskiy.paymentservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private UUID id = UUID.randomUUID();

    @Field(name = "user_id")
    private UUID userId;

    @Field(name = "order_id")
    private Long orderId;

    private PaymentStatus status;
    private Instant timestamp;

    @Field(name = "payment_amount")
    private BigDecimal paymentAmount;
}
