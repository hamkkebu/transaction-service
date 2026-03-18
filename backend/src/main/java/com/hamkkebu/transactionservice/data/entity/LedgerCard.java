package com.hamkkebu.transactionservice.data.entity;

import com.hamkkebu.boilerplate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 가계부-카드 M:N 관계 엔티티
 *
 * <p>한 개의 카드가 여러 가계부에 등록될 수 있고, 한 개의 가계부에 여러 카드가 등록될 수 있습니다.</p>
 */
@Entity
@Table(name = "tbl_ledger_cards", indexes = {
    @Index(name = "idx_ledger_card_ledger_id", columnList = "ledger_id"),
    @Index(name = "idx_ledger_card_card_id", columnList = "card_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_ledger_card", columnNames = {"ledger_id", "card_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_card_id")
    private Long ledgerCardId;

    /**
     * 가계부 ID
     */
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    /**
     * 카드 ID (Card FK)
     */
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    /**
     * 카드를 연결한 사용자 ID
     */
    @Column(name = "linked_by", nullable = false)
    private Long linkedBy;

    /**
     * 마지막 동기화 일자
     * <p>승인내역 조회 시 이 날짜 이후의 내역만 가져옴</p>
     */
    @Column(name = "last_synced_date", length = 10)
    private String lastSyncedDate;

    /**
     * Card 엔티티와의 관계
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", insertable = false, updatable = false)
    private Card card;
}
