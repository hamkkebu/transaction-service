package com.hamkkebu.transactionservice.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * <p>ledger-service에서 Outbox 패턴(StringSerializer)으로 발행한
 * 가계부 관련 이벤트를 수신합니다.</p>
 *
 * <p>Outbox 패턴에서 payload가 이미 JSON 문자열이므로
 * StringDeserializer로 수신 후 수동 JSON 파싱합니다.</p>
 */
@Slf4j
@Component
public class LedgerEventConsumer extends AbstractLedgerEventConsumer<Ledger> {

    private final ObjectMapper objectMapper;

    public LedgerEventConsumer(LedgerRepository ledgerRepository, ObjectMapper objectMapper) {
        super(ledgerRepository);
        this.objectMapper = objectMapper;
    }

    /**
     * 가계부 이벤트 처리 (LEDGER_CREATED, LEDGER_UPDATED, LEDGER_DELETED)
     *
     * <p>StringDeserializer로 수신한 JSON 문자열을 Map으로 파싱 후 처리합니다.</p>
     */
    @KafkaListener(
            topics = "${kafka.topics.ledger-events:ledger.events}",
            groupId = "transaction-service-group",
            containerFactory = "outboxEventListenerContainerFactory"
    )
    @Transactional
    public void handleLedgerEvent(String payload) {
        try {
            log.info("[Kafka Consumer] Received ledger event payload (length={})", payload.length());

            // 이중 인코딩 처리: Producer가 JsonSerializer로 JSON 문자열을 한번 더 직렬화한 경우
            // 페이로드가 '"{ ... }"' (바깥 따옴표 포함) 형태로 도착함
            String jsonString = payload;
            if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                log.debug("[Kafka Consumer] Detected double-encoded payload, unwrapping...");
                jsonString = objectMapper.readValue(jsonString, String.class);
            }

            Map<String, Object> eventData = objectMapper.readValue(jsonString,
                    new TypeReference<Map<String, Object>>() {});
            processLedgerEvent(eventData);
        } catch (Exception e) {
            log.error("[Kafka Consumer] Failed to parse ledger event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse ledger event", e);
        }
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
