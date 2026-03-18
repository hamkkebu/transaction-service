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
 * 은행 계좌 엔티티
 *
 * <p>계정(사용자)이 소유한 은행 계좌 정보를 저장합니다.</p>
 * <p>Codef를 통해 연동한 은행 계좌의 기본 정보를 최소한으로 저장합니다.</p>
 */
@Entity
@Table(name = "tbl_bank_accounts", indexes = {
    @Index(name = "idx_bank_account_account_id", columnList = "account_id"),
    @Index(name = "idx_bank_account_connected_id", columnList = "connected_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class BankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_account_id")
    private Long bankAccountId;

    /**
     * 계좌 소유자 계정 ID
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /**
     * Codef Connected ID
     * <p>은행 계좌 등록 시 발급되는 식별자</p>
     */
    @Column(name = "connected_id", nullable = false, length = 100)
    private String connectedId;

    /**
     * 은행 기관 코드
     * <p>예: 0004 = 국민, 0011 = NH, 0020 = 우리 등</p>
     */
    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    /**
     * 계좌번호
     */
    @Column(name = "account_number", length = 30)
    private String accountNumber;

    /**
     * 사용자 지정 계좌 별칭
     * <p>예: "급여 계좌", "생활 계좌"</p>
     */
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    /**
     * 마스킹된 계좌번호 (뒷 4자리만)
     * <p>예: "1234"</p>
     */
    @Column(name = "account_no_masked", length = 20)
    private String accountNoMasked;
}
