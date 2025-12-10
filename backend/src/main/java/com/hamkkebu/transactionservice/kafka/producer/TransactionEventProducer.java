package com.hamkkebu.transactionservice.kafka.producer;

import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 거래 이벤트 Kafka Producer
 *
 * <p>거래 생성/수정/삭제 시 이벤트를 발행합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${kafka.topics.transaction-events:transaction.events}")
    private String transactionEventsTopic;

    /**
     * 거래 생성 이벤트 발행
     */
    public void publishTransactionCreated(Transaction transaction) {
        TransactionEvent event = TransactionEvent.created(
                transaction.getId(),
                transaction.getLedgerId(),
                transaction.getUserId(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getTransactionDate(),
                transaction.getMemo()
        );

        publishEvent(event);
    }

    /**
     * 거래 수정 이벤트 발행
     */
    public void publishTransactionUpdated(Transaction transaction) {
        TransactionEvent event = TransactionEvent.updated(
                transaction.getId(),
                transaction.getLedgerId(),
                transaction.getUserId(),
                transaction.getType().name(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCategory(),
                transaction.getTransactionDate(),
                transaction.getMemo()
        );

        publishEvent(event);
    }

    /**
     * 거래 삭제 이벤트 발행
     */
    public void publishTransactionDeleted(Transaction transaction) {
        TransactionEvent event = TransactionEvent.deleted(
                transaction.getId(),
                transaction.getLedgerId(),
                transaction.getUserId()
        );

        publishEvent(event);
    }

    private void publishEvent(TransactionEvent event) {
        log.info("[Kafka Producer] Publishing transaction event: eventType={}, eventId={}, transactionId={}",
                event.getEventType(), event.getEventId(), event.getTransactionId());

        kafkaTemplate.send(transactionEventsTopic, event.getTransactionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka Producer] Failed to publish event: eventId={}, error={}",
                                event.getEventId(), ex.getMessage(), ex);
                    } else {
                        log.info("[Kafka Producer] Event published successfully: eventId={}, topic={}, partition={}",
                                event.getEventId(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
