package com.hamkkebu.transactionservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p>ledger-service에서 Outbox 패턴(StringSerializer)으로 발행한
 * 가계부 공유 관련 이벤트를 수신합니다.</p>
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

    private final ObjectMapper objectMapper;

    public LedgerShareEventConsumer(LedgerShareRepository ledgerShareRepository, ObjectMapper objectMapper) {
        super(ledgerShareRepository);
        this.objectMapper = objectMapper;
    }

    /**
     * 가계부 공유 이벤트 처리
     *
     * <p>StringDeserializer로 수신한 JSON 문자열을 Map으로 파싱 후 처리합니다.</p>
     */
    @KafkaListener(
            topics = "${kafka.topics.ledger-share-events:ledger-share.events}",
            groupId = "transaction-service-group",
            containerFactory = "outboxEventListenerContainerFactory"
    )
    @Transactional
    public void handleLedgerShareEvent(String payload) {
        try {
            log.info("[Kafka Consumer] Received ledger share event payload (length={})", payload.length());

            // 이중 인코딩 처리: Producer가 JsonSerializer로 JSON 문자열을 한번 더 직렬화한 경우
            String jsonString = payload;
            if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                log.debug("[Kafka Consumer] Detected double-encoded payload, unwrapping...");
                jsonString = objectMapper.readValue(jsonString, String.class);
            }

            Map<String, Object> eventData = objectMapper.readValue(jsonString,
                    new TypeReference<Map<String, Object>>() {});
            processLedgerShareEvent(eventData);
        } catch (Exception e) {
            log.error("[Kafka Consumer] Failed to parse ledger share event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse ledger share event", e);
        }
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
