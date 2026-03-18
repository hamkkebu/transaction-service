package com.hamkkebu.transactionservice.codef.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Codef 보유카드 정보 DTO
 *
 * <p>Codef에서 조회한 카드 정보를 프론트엔드에 전달하기 위한 DTO</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodefCardInfo {

    /** Codef에서 제공하는 카드 식별 번호 */
    private String cardId;

    /** 카드명 */
    private String cardName;

    /** 마스킹된 카드번호 */
    private String cardNoMasked;

    /** 카드사 기관 코드 */
    private String organization;

    /** 카드사명 */
    private String organizationName;
}
