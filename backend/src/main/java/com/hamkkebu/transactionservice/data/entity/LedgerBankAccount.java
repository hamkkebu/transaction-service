package com.hamkkebu.transactionservice.data.entity;

import com.hamkkebu.boilerplate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 가계부-은행계좌 M:N 관계 엔티티
 *
 * <p>한 개의 은행 계좌가 여러 가계부에 등록될 수 있고, 한 개의 가계부에 여러 계좌가 등록될 수 있습니다.</p>
 */
@Entity
@Table(name = "tbl_ledger_bank_accounts", indexes = {
    @Index(name = "idx_ledger_bank_account_ledger_id", columnList = "ledger_id"),
    @Index(name = "idx_ledger_bank_account_bank_account_id", columnList = "bank_account_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ledger_bank_account", columnNames = {"ledger_id", "bank_account_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerBankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_bank_account_id")
    private Long ledgerBankAccountId;

    /**
     * 가계부 ID
     */
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    /**
     * 은행 계좌 ID (BankAccount FK)
     */
    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    /**
     * 계좌를 연결한 사용자 ID
     */
    @Column(name = "linked_by", nullable = false)
    private Long linkedBy;

    /**
     * 마지막 동기화 일자
     * <p>거래 내역 조회 시 이 날짜 이후의 내역만 가져옴</p>
     */
    @Column(name = "last_synced_date", length = 10)
    private String lastSyncedDate;

    /**
     * BankAccount 엔티티와의 관계
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", insertable = false, updatable = false)
    private BankAccount bankAccount;
}
