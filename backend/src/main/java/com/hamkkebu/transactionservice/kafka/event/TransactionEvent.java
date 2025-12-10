package com.hamkkebu.transactionservice.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 거래 이벤트 DTO
 *
 * <p>transaction-service에서 발행하는 거래 관련 이벤트입니다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    public static final String EVENT_TYPE_CREATED = "TRANSACTION_CREATED";
    public static final String EVENT_TYPE_UPDATED = "TRANSACTION_UPDATED";
    public static final String EVENT_TYPE_DELETED = "TRANSACTION_DELETED";

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;

    // 거래 정보
    private Long transactionId;
    private Long ledgerId;
    private Long userId;
    private String type;  // INCOME, EXPENSE
    private BigDecimal amount;
    private String description;
    private String category;
    private LocalDate transactionDate;
    private String memo;

    public static TransactionEvent created(Long transactionId, Long ledgerId, Long userId,
                                           String type, BigDecimal amount, String description,
                                           String category, LocalDate transactionDate, String memo) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE_CREATED)
                .timestamp(LocalDateTime.now())
                .transactionId(transactionId)
                .ledgerId(ledgerId)
                .userId(userId)
                .type(type)
                .amount(amount)
                .description(description)
                .category(category)
                .transactionDate(transactionDate)
                .memo(memo)
                .build();
    }

    public static TransactionEvent updated(Long transactionId, Long ledgerId, Long userId,
                                           String type, BigDecimal amount, String description,
                                           String category, LocalDate transactionDate, String memo) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE_UPDATED)
                .timestamp(LocalDateTime.now())
                .transactionId(transactionId)
                .ledgerId(ledgerId)
                .userId(userId)
                .type(type)
                .amount(amount)
                .description(description)
                .category(category)
                .transactionDate(transactionDate)
                .memo(memo)
                .build();
    }

    public static TransactionEvent deleted(Long transactionId, Long ledgerId, Long userId) {
        return TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE_DELETED)
                .timestamp(LocalDateTime.now())
                .transactionId(transactionId)
                .ledgerId(ledgerId)
                .userId(userId)
                .build();
    }
}
