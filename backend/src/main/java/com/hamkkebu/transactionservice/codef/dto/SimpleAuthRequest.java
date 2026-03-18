package com.hamkkebu.transactionservice.codef.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 간편인증 2차 요청 DTO (추가인증 확인)
 *
 * <p>1차 요청에서 받은 twoWayInfo와 함께
 * 사용자가 앱에서 인증을 완료한 후 2차 확인 요청을 보냅니다.</p>
 */
@Getter
@Setter
public class SimpleAuthRequest {

    @NotBlank(message = "카드사 기관 코드는 필수입니다")
    private String organization;

    /** 1차 요청에서 받은 2-way 인증 정보 */
    private Map<String, Object> twoWayInfo;
}
