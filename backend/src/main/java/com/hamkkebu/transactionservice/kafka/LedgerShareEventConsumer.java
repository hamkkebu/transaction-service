package com.hamkkebu.transactionservice.kafka;

import com.hamkkebu.boilerplate.common.ledger.consumer.AbstractLedgerShareEventConsumer;
import com.hamkkebu.transactionservice.data.entity.LedgerShare;
import com.hamkkebu.transactionservice.repository.LedgerShareRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Transaction Service 가계부 공유 이벤트 Kafka Consumer
 *
 * <p>AbstractLedgerShareEventConsumer를 상속받아 공통 로직을 재사용합니다.</p>
 * <p>ledger-service에서 발행한 가계부 공유 관련 이벤트를 수신합니다.</p>
 *
 * <p>처리하는 이벤트:</p>
 * <ul>
 *   <li>LEDGER_SHARE_CREATED: 공유 요청 동기화</li>
 *   <li>LEDGER_SHARE_ACCEPTED: 공유 수락 상태 반영</li>
 *   <li>LEDGER_SHARE_REJECTED: 공유 거절 상태 반영</li>
 *   <li>LEDGER_SHARE_DELETED: 공유 삭제 반영</li>
 * </ul>
 */
@Slf4j
@Component
public class LedgerShareEventConsumer extends AbstractLedgerShareEventConsumer<LedgerShare> {

    public LedgerShareEventConsumer(LedgerShareRepository ledgerShareRepository) {
        super(ledgerShareRepository);
    }

    /**
     * 가계부 공유 이벤트 처리
     */
    @KafkaListener(
            topics = "${kafka.topics.ledger-share-events:ledger-share.events}",
            groupId = "transaction-service-group",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleLedgerShareEvent(Map<String, Object> eventData) {
        processLedgerShareEvent(eventData);
    }

    @Override
    protected LedgerShare createLedgerShareEntity(Map<String, Object> eventData) {
        return LedgerShare.builder()
                .ledgerShareId(extractLedgerShareId(eventData))
                .ledgerId(extractLedgerId(eventData))
                .ownerId(extractOwnerId(eventData))
                .sharedUserId(extractSharedUserId(eventData))
                .status(extractStatus(eventData))
                .permission(extractPermission(eventData))
                .build();
    }
}
