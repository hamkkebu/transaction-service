package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.boilerplate.common.ledger.repository.SyncedLedgerShareRepository;
import com.hamkkebu.transactionservice.data.entity.LedgerShare;
import org.springframework.stereotype.Repository;

/**
 * Transaction Service 가계부 공유 Repository
 *
 * <p>SyncedLedgerShareRepository를 상속받아 기본 쿼리 메서드를 사용합니다.</p>
 * <p>공유받은 가계부에 대한 접근 권한 검증에 사용됩니다.</p>
 */
@Repository
public interface LedgerShareRepository extends SyncedLedgerShareRepository<LedgerShare> {
    // SyncedLedgerShareRepository에서 제공하는 메서드:
    // - findByLedgerIdAndIsDeletedFalse(Long)
    // - findBySharedUserIdAndStatusAndIsDeletedFalse(Long, ShareStatus)
    // - findByLedgerIdAndSharedUserIdAndIsDeletedFalse(Long, Long)
    // - findByLedgerIdAndSharedUserIdAndStatusAndIsDeletedFalse(Long, Long, ShareStatus)
    // - findByOwnerIdAndIsDeletedFalse(Long)
    // - countByLedgerIdAndStatusAndIsDeletedFalse(Long, ShareStatus)
    // - existsByLedgerIdAndSharedUserIdAndStatusAndIsDeletedFalse(Long, Long, ShareStatus)
}
