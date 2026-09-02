package com.dsavitskiy.paymentservice.repository;

import com.dsavitskiy.paymentservice.dto.PaymentSearchDto;
import com.dsavitskiy.paymentservice.dto.PaymentSumResponseDto;
import com.dsavitskiy.paymentservice.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentCustomRepositoryImpl implements PaymentCustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Payment> searchPayments(PaymentSearchDto filter) {
        Query query = new Query();
        if (filter.userId() != null) {
            query.addCriteria(Criteria.where("user_id").is(filter.userId()));
        }
        if (filter.orderId() != null) {
            query.addCriteria(Criteria.where("order_id").is(filter.orderId()));
        }
        if (filter.status() != null) {
            query.addCriteria(Criteria.where("status").is(filter.status().name()));
        }

        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        return mongoTemplate.find(query, Payment.class);
    }

    @Override
    public PaymentSumResponseDto getTotalAmountByUserAndDateRange(UUID userId, Instant startDate, Instant endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("user_id").is(userId)
                                .and("timestamp").gte(startDate).lte(endDate)
                ),
                Aggregation.group()
                        .sum("payment_amount").as("totalAmount")
                        .count().as("paymentsCount")
        );

        AggregationResults<PaymentSumResponseDto> results =
                mongoTemplate.aggregate(aggregation, "payments", PaymentSumResponseDto.class);

        return results.getUniqueMappedResult();
    }

    @Override
    public PaymentSumResponseDto getTotalAmountByDateRange(Instant startDate, Instant endDate) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("timestamp").gte(startDate).lte(endDate)
                ),
                Aggregation.group()
                        .sum("payment_amount").as("totalAmount")
                        .count().as("paymentsCount")
        );

        AggregationResults<PaymentSumResponseDto> results =
                mongoTemplate.aggregate(aggregation, "payments", PaymentSumResponseDto.class);

        return results.getUniqueMappedResult();
    }
}