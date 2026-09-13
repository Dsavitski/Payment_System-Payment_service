package com.dsavitskiy.paymentservice.repository;

import com.dsavitskiy.paymentservice.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, UUID>, PaymentCustomRepository {
}
