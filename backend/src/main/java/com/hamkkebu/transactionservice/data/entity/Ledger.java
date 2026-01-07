package com.hamkkebu.transactionservice.data.entity;

import com.hamkkebu.boilerplate.common.ledger.entity.SyncedLedger;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Transaction Service Ledger 엔티티 (Ledger Service에서 동기화)
 *
 * <p>ledger-service에서 Kafka 이벤트를 통해 동기화된 가계부 정보를 저장합니다.</p>
 * <p>거래 생성 시 가계부 소유권 검증에 사용합니다.</p>
 *
 * <p>SyncedLedger를 상속받아 공통 필드와 메서드를 재사용합니다.</p>
 */
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "tbl_ledgers")
public class Ledger extends SyncedLedger {
    // 서비스별 추가 필드가 필요한 경우 여기에 정의
}
