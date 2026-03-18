package com.hamkkebu.transactionservice.codef.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 동기화 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /** 새로 추가된 거래 수 */
    private int insertedCount;

    /** 업데이트된 거래 수 (금액 변경, 취소 등) */
    private int updatedCount;

    /** 건너뛴 거래 수 (이미 존재하고 변경 없음) */
    private int skippedCount;

    /** 동기화 기간 */
    private String syncPeriod;
}
