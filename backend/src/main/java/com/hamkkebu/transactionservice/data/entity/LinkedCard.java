package com.hamkkebu.transactionservice.data.entity;

import com.hamkkebu.boilerplate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Codef 연동 카드 엔티티
 *
 * <p>사용자가 Codef를 통해 연동한 카드 정보를 최소한으로 저장합니다.</p>
 * <p>카드사 비밀번호, 카드 전체 번호 등 민감 정보는 저장하지 않습니다.</p>
 */
@Entity
@Table(name = "tbl_linked_cards", indexes = {
    @Index(name = "idx_linked_card_user_id", columnList = "user_id"),
    @Index(name = "idx_linked_card_ledger_id", columnList = "ledger_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linked_card_id")
    private Long linkedCardId;

    /**
     * 사용자 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 연결할 가계부 ID
     */
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    /**
     * Codef Connected ID
     * <p>카드사 계정 등록 시 발급되는 식별자</p>
     */
    @Column(name = "connected_id", nullable = false, length = 100)
    private String connectedId;

    /**
     * 카드사 기관 코드
     * <p>예: 0309 = 신한, 0302 = 현대, 0306 = 삼성 등</p>
     */
    @Column(name = "organization", nullable = false, length = 10)
    private String organization;

    /**
     * 사용자 지정 카드 별칭
     * <p>예: "신한 주카드", "현대 생활카드"</p>
     */
    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    /**
     * 마스킹된 카드번호 (뒷 4자리만)
     * <p>예: "1234"</p>
     */
    @Column(name = "card_no_masked", length = 20)
    private String cardNoMasked;

    /**
     * Codef에서 제공하는 카드 식별 번호
     * <p>보유카드 조회 시 반환되는 카드 구분 값</p>
     */
    @Column(name = "card_id", length = 100)
    private String cardId;

    /**
     * 마지막 동기화 일자
     * <p>승인내역 조회 시 이 날짜 이후의 내역만 가져옴</p>
     */
    @Column(name = "last_synced_date", length = 10)
    private String lastSyncedDate;
}
