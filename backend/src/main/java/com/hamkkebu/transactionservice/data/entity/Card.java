package com.hamkkebu.transactionservice.data.entity;

import com.hamkkebu.boilerplate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * 카드 엔티티
 *
 * <p>계정(사용자)이 소유한 카드 정보를 저장합니다.</p>
 * <p>Codef를 통해 연동한 카드의 기본 정보를 최소한으로 저장합니다.</p>
 */
@Entity
@Table(name = "tbl_cards", indexes = {
    @Index(name = "idx_card_account_id", columnList = "account_id"),
    @Index(name = "idx_card_connected_id", columnList = "connected_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    /**
     * 카드 소유자 계정 ID
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

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
     * Codef에서 제공하는 카드 식별 번호
     * <p>보유카드 조회 시 반환되는 카드 구분 값</p>
     */
    @Column(name = "card_identifier", length = 50)
    private String cardIdentifier;

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
}
