package com.dsavitskiy.paymentservice.kafka;

import com.dsavitskiy.paymentservice.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic.payment-created}")
    private String topicName;

    public void sendPaymentCreatedEvent(PaymentCreatedEvent event, Long orderId) {
        String key = String.valueOf(orderId);

        log.info("Sending PaymentCreated event to topic [{}]: paymentId={}, orderId={}, status={}",
                topicName, event.paymentId(), event.orderId(), event.status());

        CompletableFuture<SendResult<String, PaymentCreatedEvent>> future =
                kafkaTemplate.send(topicName, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent event to Kafka. Partition: {}, Offset: {}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send event to Kafka for orderId={}: {}",
                        event.orderId(), ex.getMessage());
            }
        });
    }
}