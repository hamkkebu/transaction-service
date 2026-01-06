package com.hamkkebu.transactionservice.kafka.producer;

import com.hamkkebu.boilerplate.common.publisher.OutboxEventPublisher;
import com.hamkkebu.boilerplate.data.event.TransactionCreatedEvent;
import com.hamkkebu.boilerplate.data.event.TransactionDeletedEvent;
import com.hamkkebu.boilerplate.data.event.TransactionUpdatedEvent;
import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionEventProducer 단위 테스트
 *
 * <p>Transactional Outbox 패턴을 사용하여 이벤트가 올바르게 발행되는지 테스트합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionEventProducer 테스트")
class TransactionEventProducerTest {

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private TransactionEventProducer transactionEventProducer;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // Kafka 토픽 설정
        ReflectionTestUtils.setField(transactionEventProducer, "transactionEventsTopic", "transaction.events");

        // 테스트용 거래 데이터
        transaction = Transaction.builder()
                .ledgerId(1L)
                .userId(1L)
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.valueOf(50000))
                .description("점심 식사")
                .category("식비")
                .transactionDate(LocalDate.now())
                .memo("팀 점심")
                .build();
        ReflectionTestUtils.setField(transaction, "id", 1L);
    }

    @Test
    @DisplayName("거래 생성 이벤트 발행 성공")
    void publishTransactionCreated_Success() {
        // Given
        doNothing().when(outboxEventPublisher).publish(anyString(), any(TransactionCreatedEvent.class));

        // When
        transactionEventProducer.publishTransactionCreated(transaction);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);

        verify(outboxEventPublisher).publish(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("transaction.events");

        TransactionCreatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getResourceId()).isEqualTo("1");
        assertThat(capturedEvent.getUserId()).isEqualTo("1");
        assertThat(capturedEvent.getLedgerId()).isEqualTo("1");
        assertThat(capturedEvent.getEventType()).isEqualTo("TRANSACTION_CREATED");
        assertThat(capturedEvent.getEventId()).isNotNull();
    }

    @Test
    @DisplayName("거래 수정 이벤트 발행 성공")
    void publishTransactionUpdated_Success() {
        // Given
        doNothing().when(outboxEventPublisher).publish(anyString(), any(TransactionUpdatedEvent.class));

        // When
        transactionEventProducer.publishTransactionUpdated(transaction);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TransactionUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionUpdatedEvent.class);

        verify(outboxEventPublisher).publish(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("transaction.events");

        TransactionUpdatedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getResourceId()).isEqualTo("1");
        assertThat(capturedEvent.getUserId()).isEqualTo("1");
        assertThat(capturedEvent.getLedgerId()).isEqualTo("1");
        assertThat(capturedEvent.getEventType()).isEqualTo("TRANSACTION_UPDATED");
    }

    @Test
    @DisplayName("거래 삭제 이벤트 발행 성공")
    void publishTransactionDeleted_Success() {
        // Given
        doNothing().when(outboxEventPublisher).publish(anyString(), any(TransactionDeletedEvent.class));

        // When
        transactionEventProducer.publishTransactionDeleted(transaction);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TransactionDeletedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionDeletedEvent.class);

        verify(outboxEventPublisher).publish(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("transaction.events");

        TransactionDeletedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent).isNotNull();
        assertThat(capturedEvent.getResourceId()).isEqualTo("1");
        assertThat(capturedEvent.getUserId()).isEqualTo("1");
        assertThat(capturedEvent.getLedgerId()).isEqualTo("1");
        assertThat(capturedEvent.getEventType()).isEqualTo("TRANSACTION_DELETED");
    }

    @Test
    @DisplayName("이벤트 발행 시 OutboxEventPublisher 호출 검증")
    void publishEvent_VerifyOutboxPublisherCalled() {
        // Given
        doNothing().when(outboxEventPublisher).publish(anyString(), any());

        // When
        transactionEventProducer.publishTransactionCreated(transaction);
        transactionEventProducer.publishTransactionUpdated(transaction);
        transactionEventProducer.publishTransactionDeleted(transaction);

        // Then
        verify(outboxEventPublisher, times(3)).publish(eq("transaction.events"), any());
    }

    @Test
    @DisplayName("이벤트 ID가 고유하게 생성되는지 확인")
    void publishEvents_UniqueEventIds() {
        // Given
        doNothing().when(outboxEventPublisher).publish(anyString(), any());

        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);

        // When - 두 번 호출
        transactionEventProducer.publishTransactionCreated(transaction);
        transactionEventProducer.publishTransactionCreated(transaction);

        // Then
        verify(outboxEventPublisher, times(2)).publish(eq("transaction.events"), eventCaptor.capture());

        var capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents).hasSize(2);
        assertThat(capturedEvents.get(0).getEventId()).isNotEqualTo(capturedEvents.get(1).getEventId());
    }
}
