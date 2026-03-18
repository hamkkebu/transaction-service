package com.hamkkebu.transactionservice.codef.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 카드 연동 요청 DTO
 *
 * <p>보유카드 조회 후 사용자가 선택한 카드를 가계부에 연동할 때 사용</p>
 */
@Getter
@Setter
public class LinkCardRequest {

    @NotNull(message = "가계부 ID는 필수입니다")
    private Long ledgerId;

    @NotBlank(message = "Connected ID는 필수입니다")
    private String connectedId;

    @NotBlank(message = "카드사 기관 코드는 필수입니다")
    private String organization;

    /** Codef에서 제공하는 카드 식별 번호 */
    @NotBlank(message = "카드 ID는 필수입니다")
    private String cardId;

    /** 사용자 지정 카드 별칭 */
    @NotBlank(message = "카드 별칭은 필수입니다")
    private String cardName;

    /** 마스킹된 카드번호 */
    private String cardNoMasked;
}
