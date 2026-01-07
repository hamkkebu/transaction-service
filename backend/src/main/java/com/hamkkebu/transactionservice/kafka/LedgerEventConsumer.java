package com.hamkkebu.transactionservice.kafka;

import com.hamkkebu.boilerplate.common.ledger.consumer.AbstractLedgerEventConsumer;
import com.hamkkebu.transactionservice.data.entity.Ledger;
import com.hamkkebu.transactionservice.repository.LedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Transaction Service 가계부 이벤트 Kafka Consumer
 *
 * <p>AbstractLedgerEventConsumer를 상속받아 공통 로직을 재사용합니다.</p>
 * <p>ledger-service에서 발행한 가계부 관련 이벤트를 수신합니다.</p>
 */
@Slf4j
@Component
public class LedgerEventConsumer extends AbstractLedgerEventConsumer<Ledger> {

    public LedgerEventConsumer(LedgerRepository ledgerRepository) {
        super(ledgerRepository);
    }

    /**
     * 가계부 이벤트 처리 (LEDGER_CREATED, LEDGER_UPDATED, LEDGER_DELETED)
     */
    @KafkaListener(
            topics = "${kafka.topics.ledger-events:ledger.events}",
            groupId = "transaction-service-group",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleLedgerEvent(Map<String, Object> eventData) {
        processLedgerEvent(eventData);
    }

    @Override
    protected Ledger createLedgerEntity(Map<String, Object> eventData) {
        return Ledger.builder()
                .ledgerId(extractLedgerId(eventData))
                .userId(extractUserId(eventData))
                .name(extractString(eventData, "name"))
                .description(extractString(eventData, "description"))
                .currency(extractString(eventData, "currency") != null
                        ? extractString(eventData, "currency") : "KRW")
                .isDefault(extractBoolean(eventData, "isDefault"))
                .build();
    }
}
