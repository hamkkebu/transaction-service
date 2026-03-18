package com.hamkkebu.transactionservice.codef.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamkkebu.transactionservice.codef.config.CodefConfig;
import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefServiceType;
import io.codef.api.EasyCodefUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Codef API 호출 래퍼
 *
 * <p>EasyCodef 라이브러리를 감싸서 비즈니스 로직에서 편리하게 사용할 수 있도록 합니다.</p>
 * <p>토큰 관리는 EasyCodef 내부에서 자동 처리됩니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodefClient {

    private final EasyCodef easyCodef;
    private final CodefConfig codefConfig;
    private final ObjectMapper objectMapper;

    // Codef API 엔드포인트
    private static final String CARD_LIST_URL = "/v1/kr/card/p/account/card-list";
    private static final String APPROVAL_LIST_URL = "/v1/kr/card/p/account/approval-list";

    /**
     * 카드사 계정 등록 (Connected ID 발급) - ID/PW 방식
     *
     * @param organization 카드사 기관 코드
     * @param loginId      카드사 로그인 ID
     * @param loginPw      카드사 로그인 비밀번호 (평문 - RSA 암호화 후 전송)
     * @return Connected ID를 포함한 응답
     */
    public Map<String, Object> createAccount(String organization, String loginId, String loginPw) {
        try {
            List<HashMap<String, Object>> accountList = new ArrayList<>();
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("countryCode", "KR");
            accountMap.put("businessType", "CD");   // CD = 카드
            accountMap.put("clientType", "P");       // P = 개인
            accountMap.put("organization", organization);
            accountMap.put("loginType", "1");
            accountMap.put("id", loginId);
            accountMap.put("password", EasyCodefUtil.encryptRSA(loginPw, easyCodef.getPublicKey()));

            accountList.add(accountMap);

            HashMap<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("accountList", accountList);

            EasyCodefServiceType serviceType = codefConfig.getEasyCodefServiceType();
            String result = easyCodef.createAccount(serviceType, parameterMap);

            log.info("[Codef] Account created for organization={}", organization);
            return parseResponse(result);

        } catch (Exception e) {
            log.error("[Codef] Failed to create account: {}", e.getMessage(), e);
            throw new RuntimeException("Codef 계정 등록 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 카드사 계정 등록 - 간편인증 방식 (1차 요청)
     *
     * <p>간편인증 요청을 보내면 사용자의 앱에 인증 요청이 전달됩니다.
     * 응답으로 twoWayInfo가 포함되며, 사용자가 앱에서 인증 후
     * {@link #confirmSimpleAuth}로 2차 확인 요청을 보내야 합니다.</p>
     *
     * @param organization   카드사 기관 코드
     * @param loginTypeLevel 간편인증 수단 (1:카카오톡, 2:페이코, 3:삼성패스 등)
     * @return twoWayInfo를 포함한 응답 (2차 요청에 필요)
     */
    public Map<String, Object> createAccountSimpleAuth(String organization, String loginTypeLevel) {
        try {
            List<HashMap<String, Object>> accountList = new ArrayList<>();
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("countryCode", "KR");
            accountMap.put("businessType", "CD");
            accountMap.put("clientType", "P");
            accountMap.put("organization", organization);
            accountMap.put("loginType", "5");
            accountMap.put("loginTypeLevel", loginTypeLevel);

            accountList.add(accountMap);

            HashMap<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("accountList", accountList);

            EasyCodefServiceType serviceType = codefConfig.getEasyCodefServiceType();
            String result = easyCodef.createAccount(serviceType, parameterMap);

            log.info("[Codef] Simple auth requested for organization={}, level={}", organization, loginTypeLevel);
            return parseResponse(result);

        } catch (Exception e) {
            log.error("[Codef] Failed to request simple auth: {}", e.getMessage(), e);
            throw new RuntimeException("간편인증 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 간편인증 2차 확인 요청
     *
     * <p>사용자가 앱에서 인증을 완료한 후 호출합니다.
     * 1차 요청에서 받은 twoWayInfo(jobIndex, threadIndex, jti, twoWayTimestamp)를
     * 그대로 전달해야 합니다.</p>
     *
     * <p>Codef 가이드에 따르면:
     * - 추가인증 요청 시에도 동일한 Endpoint URL 사용
     * - simpleAuth="1", is2Way=true 파라미터 추가
     * - twoWayInfo는 parameterMap 레벨에 설정
     * - 계정 등록의 경우 createAccount()로 동일하게 호출</p>
     *
     * @param organization 카드사 기관 코드
     * @param twoWayInfo   1차 요청 응답의 data에서 추출한 twoWayInfo
     *                     (jobIndex, threadIndex, jti, twoWayTimestamp 포함)
     * @return Connected ID를 포함한 응답
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirmSimpleAuth(String organization, Map<String, Object> twoWayInfo) {
        try {
            List<HashMap<String, Object>> accountList = new ArrayList<>();
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("countryCode", "KR");
            accountMap.put("businessType", "CD");
            accountMap.put("clientType", "P");
            accountMap.put("organization", organization);
            accountMap.put("loginType", "5");
            accountMap.put("simpleAuth", "1");
            accountMap.put("is2Way", true);

            accountList.add(accountMap);

            HashMap<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("accountList", accountList);

            // twoWayInfo는 parameterMap 레벨에 설정 (Codef 가이드 기준)
            if (twoWayInfo != null) {
                HashMap<String, Object> twoWayInfoMap = new HashMap<>();
                twoWayInfoMap.put("jobIndex", twoWayInfo.get("jobIndex"));
                twoWayInfoMap.put("threadIndex", twoWayInfo.get("threadIndex"));
                twoWayInfoMap.put("jti", twoWayInfo.get("jti"));
                twoWayInfoMap.put("twoWayTimestamp", twoWayInfo.get("twoWayTimestamp"));
                parameterMap.put("twoWayInfo", twoWayInfoMap);
            }

            EasyCodefServiceType serviceType = codefConfig.getEasyCodefServiceType();
            String result = easyCodef.createAccount(serviceType, parameterMap);

            log.info("[Codef] Simple auth confirmed for organization={}", organization);
            return parseResponse(result);

        } catch (Exception e) {
            log.error("[Codef] Failed to confirm simple auth: {}", e.getMessage(), e);
            throw new RuntimeException("간편인증 확인 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 개인 보유카드 목록 조회
     *
     * @param connectedId  Connected ID
     * @param organization 카드사 기관 코드
     * @return 보유카드 목록
     */
    public Map<String, Object> getCardList(String connectedId, String organization) {
        try {
            HashMap<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("connectedId", connectedId);
            parameterMap.put("organization", organization);
            parameterMap.put("birthDate", "");
            parameterMap.put("inquiryType", "0");

            EasyCodefServiceType serviceType = codefConfig.getEasyCodefServiceType();
            String result = easyCodef.requestProduct(CARD_LIST_URL, serviceType, parameterMap);

            log.info("[Codef] Card list retrieved for organization={}", organization);
            return parseResponse(result);

        } catch (Exception e) {
            log.error("[Codef] Failed to get card list: {}", e.getMessage(), e);
            throw new RuntimeException("보유카드 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 카드 승인내역 조회
     *
     * @param connectedId  Connected ID
     * @param organization 카드사 기관 코드
     * @param startDate    조회 시작일 (yyyyMMdd)
     * @param endDate      조회 종료일 (yyyyMMdd)
     * @param cardId       카드 식별 번호 (선택)
     * @return 승인내역 목록
     */
    public Map<String, Object> getApprovalList(String connectedId, String organization,
                                                String startDate, String endDate,
                                                String cardId) {
        try {
            HashMap<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("connectedId", connectedId);
            parameterMap.put("organization", organization);
            parameterMap.put("startDate", startDate);
            parameterMap.put("endDate", endDate);
            parameterMap.put("orderBy", "0"); // 0: 최신순
            parameterMap.put("inquiryType", "0");

            if (cardId != null && !cardId.isEmpty()) {
                parameterMap.put("cardNo", cardId);
            }

            EasyCodefServiceType serviceType = codefConfig.getEasyCodefServiceType();
            String result = easyCodef.requestProduct(APPROVAL_LIST_URL, serviceType, parameterMap);

            log.info("[Codef] Approval list retrieved for organization={}, period={}-{}",
                    organization, startDate, endDate);
            return parseResponse(result);

        } catch (Exception e) {
            log.error("[Codef] Failed to get approval list: {}", e.getMessage(), e);
            throw new RuntimeException("승인내역 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Codef Connected ID에 추가 계정 등록
     */
    public Map<String, Object> addAccount(String connectedId, String organization,
                                           String loginId, String loginPw) {
        try {
            List<HashMap<String, Object>> accountList = new ArrayList<>();
            HashMap<String, Object> accountMap = new HashMap<>();

            accountMap.put("countryCode", "KR");
            accountMap.put("businessType", "CD");
            accountMap.put("clientType", "P");
            accountMap.put("organization", organization);
            accountMap.put("loginType", "1");
            accountMap.put("id", loginId);
            accountMap.put("password", EasyCodefUtil.encryptRSA(loginPw, easyCodef.getPublicKey()));

            accountList.add(accountMap);

            HashMap<String, Object> parameterMap = new HashMap<>();
            parameterMap.put("connectedId", connectedId);
            parameterMap.put("accountList", accountList);

            EasyCodefServiceType serviceType = codefConfig.getEasyCodefServiceType();
            String result = easyCodef.addAccount(serviceType, parameterMap);

            log.info("[Codef] Account added to connectedId for organization={}", organization);
            return parseResponse(result);

        } catch (Exception e) {
            log.error("[Codef] Failed to add account: {}", e.getMessage(), e);
            throw new RuntimeException("Codef 계정 추가 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Codef API 응답 문자열을 Map으로 파싱
     */
    private Map<String, Object> parseResponse(String result) {
        try {
            return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("[Codef] Failed to parse response: {}", e.getMessage());
            throw new RuntimeException("Codef 응답 파싱 실패", e);
        }
    }
}
