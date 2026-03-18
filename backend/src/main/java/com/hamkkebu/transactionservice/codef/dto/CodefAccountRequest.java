package com.hamkkebu.transactionservice.codef.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 카드사 계정 등록 요청 DTO
 *
 * <p>loginType에 따라 필요한 필드가 다릅니다:</p>
 * <ul>
 *   <li>"1" (ID/PW): loginId, loginPw 필수</li>
 *   <li>"5" (간편인증): loginTypeLevel 필수, loginId/loginPw 불필요</li>
 * </ul>
 */
@Getter
@Setter
public class CodefAccountRequest {

    @NotBlank(message = "카드사 기관 코드는 필수입니다")
    private String organization;

    /**
     * 로그인 타입
     * "1" = ID/PW 로그인 (기본값)
     * "5" = 간편인증
     */
    private String loginType = "1";

    /**
     * 간편인증 수단 (loginType="5" 일 때 필수)
     * 1:카카오톡, 2:페이코, 3:삼성패스, 4:KB모바일,
     * 5:통신사PASS, 6:네이버, 7:신한인증서, 8:토스
     */
    private String loginTypeLevel;

    /** 카드사 로그인 ID (loginType="1" 일 때 필수) */
    private String loginId;

    /** 카드사 로그인 비밀번호 (loginType="1" 일 때 필수) */
    private String loginPw;
}
