package com.hamkkebu.transactionservice.codef.service;

import com.hamkkebu.transactionservice.codef.client.CodefClient;
import com.hamkkebu.transactionservice.codef.dto.SyncResult;
import com.hamkkebu.transactionservice.data.entity.LinkedCard;
import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionSourceType;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import com.hamkkebu.transactionservice.repository.LinkedCardRepository;
import com.hamkkebu.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Codef 승인내역 동기화 서비스
 *
 * <p>Codef에서 카드 승인내역을 가져와 Transaction으로 변환/저장합니다.</p>
 * <p>externalApprovalNo 기준으로 중복 체크 및 업데이트를 수행합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodefSyncService {

    private final CodefClient codefClient;
    private final LinkedCardRepository linkedCardRepository;
    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter CODEF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DEFAULT_SYNC_DAYS = 30;

    /**
     * 특정 연동 카드의 승인내역 동기화
     *
     * @param userId       사용자 ID
     * @param linkedCardId 연동 카드 ID
     * @return 동기화 결과 (추가/업데이트/건너뛰기 건수)
     */
    @Transactional
    public SyncResult syncCard(Long userId, Long linkedCardId) {
        LinkedCard card = linkedCardRepository
                .findByLinkedCardIdAndUserIdAndIsDeletedFalse(linkedCardId, userId)
                .orElseThrow(() -> new RuntimeException("연동 카드를 찾을 수 없습니다."));

        // 동기화 기간 설정: 마지막 동기화일 또는 기본 30일 전부터
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        if (card.getLastSyncedDate() != null && !card.getLastSyncedDate().isEmpty()) {
            startDate = LocalDate.parse(card.getLastSyncedDate(), CODEF_DATE_FORMAT);
        } else {
            startDate = endDate.minusDays(DEFAULT_SYNC_DAYS);
        }

        String startDateStr = startDate.format(CODEF_DATE_FORMAT);
        String endDateStr = endDate.format(CODEF_DATE_FORMAT);

        log.info("[CodefSyncService] Syncing card: linkedCardId={}, period={}-{}",
                linkedCardId, startDateStr, endDateStr);

        // Codef 승인내역 조회
        Map<String, Object> response = codefClient.getApprovalList(
                card.getConnectedId(),
                card.getOrganization(),
                startDateStr,
                endDateStr,
                card.getCardId()
        );

        // 응답 검증
        Map<String, Object> resultInfo = extractResult(response);
        String code = (String) resultInfo.get("code");
        if (!"CF-00000".equals(code)) {
            String message = (String) resultInfo.get("message");
            throw new RuntimeException("승인내역 조회 실패: " + message);
        }

        // 승인내역을 거래로 변환/저장
        SyncResult syncResult = processApprovalList(response, card);

        // 마지막 동기화 일자 갱신
        card.setLastSyncedDate(endDateStr);
        linkedCardRepository.save(card);

        log.info("[CodefSyncService] Sync completed: linkedCardId={}, inserted={}, updated={}, skipped={}",
                linkedCardId, syncResult.getInsertedCount(),
                syncResult.getUpdatedCount(), syncResult.getSkippedCount());

        return syncResult;
    }

    /**
     * 승인내역 응답을 파싱하여 Transaction으로 변환/저장
     */
    @SuppressWarnings("unchecked")
    private SyncResult processApprovalList(Map<String, Object> response, LinkedCard card) {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        Object data = response.get("data");
        if (!(data instanceof List)) {
            return SyncResult.builder()
                    .insertedCount(0).updatedCount(0).skippedCount(0)
                    .syncPeriod(card.getLastSyncedDate() + " ~ " + LocalDate.now().format(CODEF_DATE_FORMAT))
                    .build();
        }

        List<Map<String, Object>> approvalList = (List<Map<String, Object>>) data;

        for (Map<String, Object> approval : approvalList) {
            try {
                String approvalNo = getStringValue(approval, "resApprovalNo");
                if (approvalNo == null || approvalNo.isEmpty()) {
                    skipped++;
                    continue;
                }

                // 기존 거래 확인 (승인번호 기준 매칭)
                Optional<Transaction> existingOpt = transactionRepository
                        .findByExternalApprovalNoAndLinkedCardIdAndIsDeletedFalse(
                                approvalNo, card.getLinkedCardId());

                BigDecimal amount = parseAmount(getStringValue(approval, "resApprovalAmount"));
                String storeName = getStringValue(approval, "resStoreName");
                String approvalDate = getStringValue(approval, "resApprovalDate");
                String storeCategory = getStringValue(approval, "resStoreCategory");

                if (existingOpt.isPresent()) {
                    // 기존 거래 업데이트 (금액 변경 등)
                    Transaction existing = existingOpt.get();
                    if (!existing.getAmount().equals(amount)) {
                        existing.setAmount(amount);
                        existing.setDescription(storeName);
                        existing.setCategory(storeCategory);
                        transactionRepository.save(existing);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    // 신규 거래 추가
                    Transaction transaction = Transaction.builder()
                            .ledgerId(card.getLedgerId())
                            .userId(card.getUserId())
                            .type(TransactionType.EXPENSE) // 카드 승인내역은 기본 지출
                            .amount(amount)
                            .description(storeName)
                            .category(storeCategory != null && !storeCategory.isEmpty()
                                    ? storeCategory : "카드결제")
                            .transactionDate(parseDate(approvalDate))
                            .memo("Codef 자동 연동")
                            .sourceType(TransactionSourceType.CODEF)
                            .externalApprovalNo(approvalNo)
                            .linkedCardId(card.getLinkedCardId())
                            .build();

                    transactionRepository.save(transaction);
                    inserted++;
                }
            } catch (Exception e) {
                log.warn("[CodefSyncService] Failed to process approval item: {}", e.getMessage());
                skipped++;
            }
        }

        String period = card.getLastSyncedDate() + " ~ " + LocalDate.now().format(CODEF_DATE_FORMAT);
        return SyncResult.builder()
                .insertedCount(inserted)
                .updatedCount(updated)
                .skippedCount(skipped)
                .syncPeriod(period)
                .build();
    }

    /**
     * Codef 응답에서 result 객체 추출
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(Map<String, Object> response) {
        Object result = response.get("result");
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        throw new RuntimeException("Codef 응답에서 result를 찾을 수 없습니다.");
    }

    /**
     * 문자열 금액을 BigDecimal로 변환
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 콤마 제거 후 변환
        String cleaned = amountStr.replaceAll("[^0-9.-]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Codef 날짜 문자열(yyyyMMdd)을 LocalDate로 변환
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr, CODEF_DATE_FORMAT);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
