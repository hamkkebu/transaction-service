package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.boilerplate.common.ledger.repository.SyncedLedgerRepository;
import com.hamkkebu.transactionservice.data.entity.Ledger;
import org.springframework.stereotype.Repository;

/**
 * Transaction Service Ledger Repository
 *
 * <p>SyncedLedgerRepository를 상속받아 공통 메서드를 재사용합니다.</p>
 * <p>가계부 소유권 검증 등에 사용됩니다.</p>
 */
@Repository
public interface LedgerRepository extends SyncedLedgerRepository<Ledger> {
    // 서비스별 추가 메서드가 필요한 경우 여기에 정의
}
