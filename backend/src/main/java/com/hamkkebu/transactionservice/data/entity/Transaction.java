package com.hamkkebu.transactionservice.data.entity;

import com.hamkkebu.boilerplate.common.entity.BaseEntity;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionSourceType;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tbl_transactions", indexes = {
    @Index(name = "idx_ledger_id", columnList = "ledger_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 거래를 기록한 계정 ID
     */
    @Column(name = "account_id")
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "memo", length = 1000)
    private String memo;

    // ==================== Codef 연동 필드 ====================

    /**
     * 거래 내역 출처 (MANUAL: 수기 입력, CODEF: 자동 연동)
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private TransactionSourceType sourceType = TransactionSourceType.MANUAL;

    /**
     * Codef 승인번호 (중복 체크 및 업데이트 매칭용)
     * <p>CODEF 연동 거래에만 값이 존재</p>
     */
    @Column(name = "external_approval_no", length = 100)
    private String externalApprovalNo;

    /**
     * 연동 카드 ID (LinkedCard FK)
     * <p>CODEF 연동 거래에만 값이 존재, MANUAL이면 null</p>
     * <p>마이그레이션 기간 동안의 하위 호환성 유지용</p>
     */
    @Column(name = "linked_card_id")
    private Long linkedCardId;

    /**
     * 카드 ID (Card FK)
     * <p>CODEF 연동 카드 거래의 경우 해당 카드 정보</p>
     */
    @Column(name = "card_id")
    private Long cardId;

    /**
     * 은행 계좌 ID (BankAccount FK)
     * <p>CODEF 연동 은행 거래의 경우 해당 계좌 정보</p>
     */
    @Column(name = "bank_account_id")
    private Long bankAccountId;
}
