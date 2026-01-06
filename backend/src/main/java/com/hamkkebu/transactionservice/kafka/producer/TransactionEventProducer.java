package com.hamkkebu.transactionservice.kafka.producer;

import com.hamkkebu.boilerplate.common.publisher.OutboxEventPublisher;
import com.hamkkebu.boilerplate.data.event.TransactionCreatedEvent;
import com.hamkkebu.boilerplate.data.event.TransactionDeletedEvent;
import com.hamkkebu.boilerplate.data.event.TransactionUpdatedEvent;
import com.hamkkebu.transactionservice.data.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 거래 이벤트 Producer (Transactional Outbox 패턴)
 *
 * <p>거래 생성/수정/삭제 시 Outbox 테이블에 이벤트를 저장합니다.</p>
 * <p>실제 Kafka 발행은 OutboxEventScheduler가 비동기로 처리합니다.</p>
 *
 * <p>장점:</p>
 * <ul>
 *   <li>DB 트랜잭션과 이벤트 발행의 원자성 보장</li>
 *   <li>이벤트 발행 실패 시에도 데이터 손실 방지</li>
 *   <li>재시도 메커니즘으로 일시적 장애 대응</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${kafka.topics.transaction-events:transaction.events}")
    private String transactionEventsTopic;

    /**
     * 거래 생성 이벤트 발행 (Outbox 테이블에 저장)
     *
     * <p>반드시 @Transactional 메서드 내에서 호출해야 합니다.</p>
     */
    public void publishTransactionCreated(Transaction transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(transaction.getId().toString())
                .userId(transaction.getUserId().toString())
                .ledgerId(transaction.getLedgerId().toString())
                .build();

        outboxEventPublisher.publish(transactionEventsTopic, event);

        log.info("[Outbox] Transaction created event saved: eventId={}, transactionId={}, ledgerId={}",
                event.getEventId(), transaction.getId(), transaction.getLedgerId());
    }

    /**
     * 거래 수정 이벤트 발행 (Outbox 테이블에 저장)
     *
     * <p>반드시 @Transactional 메서드 내에서 호출해야 합니다.</p>
     */
    public void publishTransactionUpdated(Transaction transaction) {
        TransactionUpdatedEvent event = TransactionUpdatedEvent.builder()
                .transactionId(transaction.getId().toString())
                .userId(transaction.getUserId().toString())
                .ledgerId(transaction.getLedgerId().toString())
                .build();

        outboxEventPublisher.publish(transactionEventsTopic, event);

        log.info("[Outbox] Transaction updated event saved: eventId={}, transactionId={}, ledgerId={}",
                event.getEventId(), transaction.getId(), transaction.getLedgerId());
    }

    /**
     * 거래 삭제 이벤트 발행 (Outbox 테이블에 저장)
     *
     * <p>반드시 @Transactional 메서드 내에서 호출해야 합니다.</p>
     */
    public void publishTransactionDeleted(Transaction transaction) {
        TransactionDeletedEvent event = TransactionDeletedEvent.builder()
                .transactionId(transaction.getId().toString())
                .userId(transaction.getUserId().toString())
                .ledgerId(transaction.getLedgerId().toString())
                .build();

        outboxEventPublisher.publish(transactionEventsTopic, event);

        log.info("[Outbox] Transaction deleted event saved: eventId={}, transactionId={}, ledgerId={}",
                event.getEventId(), transaction.getId(), transaction.getLedgerId());
    }
}
