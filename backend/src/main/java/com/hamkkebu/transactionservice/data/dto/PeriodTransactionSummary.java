package com.hamkkebu.transactionservice.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 기간별 거래 요약 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodTransactionSummary {

    /**
     * 가계부 ID
     */
    private Long ledgerId;

    /**
     * 조회 기간 유형 (DAILY, MONTHLY, YEARLY)
     */
    private PeriodType periodType;

    /**
     * 조회 시작일
     */
    private LocalDate startDate;

    /**
     * 조회 종료일
     */
    private LocalDate endDate;

    /**
     * 해당 기간의 총 수입
     */
    private BigDecimal totalIncome;

    /**
     * 해당 기간의 총 지출
     */
    private BigDecimal totalExpense;

    /**
     * 해당 기간의 잔액 (수입 - 지출)
     */
    private BigDecimal balance;

    /**
     * 해당 기간의 거래 수
     */
    private Long transactionCount;

    /**
     * 해당 기간의 거래 목록
     */
    private List<TransactionResponse> transactions;

    /**
     * 일별/월별/년별 상세 요약 목록
     */
    private List<PeriodDetail> periodDetails;

    /**
     * 기간 유형
     */
    public enum PeriodType {
        DAILY,      // 일별
        MONTHLY,    // 월별
        YEARLY      // 년별
    }

    /**
     * 기간 상세 요약 (일별/월별/년별 각 항목)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodDetail {
        /**
         * 기간 라벨 (예: "2025-01-01", "2025-01", "2025")
         */
        private String periodLabel;

        /**
         * 기간 시작일
         */
        private LocalDate startDate;

        /**
         * 기간 종료일
         */
        private LocalDate endDate;

        /**
         * 해당 기간의 수입
         */
        private BigDecimal income;

        /**
         * 해당 기간의 지출
         */
        private BigDecimal expense;

        /**
         * 해당 기간의 잔액
         */
        private BigDecimal balance;

        /**
         * 해당 기간의 거래 수
         */
        private Long transactionCount;
    }
}
