package com.hamkkebu.transactionservice.codef.service;

import com.hamkkebu.transactionservice.data.entity.LinkedCard;
import com.hamkkebu.transactionservice.repository.LinkedCardRepository;
import com.hamkkebu.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 카드 연동 해제 후 30일 유예기간 경과 데이터 정리 스케줄러
 *
 * <p>soft delete된 LinkedCard와 해당 거래 내역을 30일 후 실제 삭제합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodefCleanupScheduler {

    private final LinkedCardRepository linkedCardRepository;
    private final TransactionRepository transactionRepository;

    private static final int RETENTION_DAYS = 30;

    /**
     * 매일 새벽 3시에 실행
     * <p>삭제된 지 30일 지난 연동 카드와 거래 내역을 실제 삭제합니다.</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredCards() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("[CodefCleanup] Starting cleanup for cards deleted before {}", cutoffDate);

        List<LinkedCard> expiredCards = linkedCardRepository
                .findByIsDeletedTrueAndDeletedAtBefore(cutoffDate);

        if (expiredCards.isEmpty()) {
            log.info("[CodefCleanup] No expired cards to clean up");
            return;
        }

        int totalTransactionsDeleted = 0;

        for (LinkedCard card : expiredCards) {
            // 해당 카드의 거래 내역 hard delete
            int deletedTx = transactionRepository.hardDeleteByLinkedCardId(card.getLinkedCardId());
            totalTransactionsDeleted += deletedTx;

            // 연동 카드 hard delete
            linkedCardRepository.delete(card);

            log.info("[CodefCleanup] Deleted card: linkedCardId={}, transactions={}",
                    card.getLinkedCardId(), deletedTx);
        }

        log.info("[CodefCleanup] Cleanup completed: cards={}, transactions={}",
                expiredCards.size(), totalTransactionsDeleted);
    }
}
