package com.hamkkebu.transactionservice.codef.service;

import com.hamkkebu.transactionservice.codef.client.CodefClient;
import com.hamkkebu.transactionservice.codef.dto.*;
import com.hamkkebu.transactionservice.data.entity.LinkedCard;
import com.hamkkebu.transactionservice.repository.LinkedCardRepository;
import com.hamkkebu.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Codef 카드 연동 관리 서비스
 *
 * <p>카드사 계정 등록, 보유카드 조회, 카드 연동/해제를 처리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodefCardService {

    private final CodefClient codefClient;
    private final LinkedCardRepository linkedCardRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 카드사 계정 등록 → Connected ID 발급
     *
     * <p>loginType에 따라 ID/PW 방식 또는 간편인증 방식으로 분기합니다.</p>
     *
     * @return Connected ID와 카드사 정보 (간편인증인 경우 twoWayInfo 포함)
     */
    public Map<String, Object> connectCardAccount(Long userId, CodefAccountRequest request) {
        log.info("[CodefCardService] Connecting card account: userId={}, org={}, loginType={}",
                userId, request.getOrganization(), request.getLoginType());

        Map<String, Object> result;

        if ("5".equals(request.getLoginType())) {
            // 간편인증 방식: 1차 요청 (앱 인증 요청 발송)
            result = codefClient.createAccountSimpleAuth(
                    request.getOrganization(),
                    request.getLoginTypeLevel()
            );
        } else {
            // ID/PW 방식
            result = codefClient.createAccount(
                    request.getOrganization(),
                    request.getLoginId(),
                    request.getLoginPw()
            );
        }

        // 성공 여부 확인 (간편인증의 경우 CF-03002 = 추가인증 대기)
        Map<String, Object> resultInfo = extractResult(result);
        String code = (String) resultInfo.get("code");
        if (!"CF-00000".equals(code) && !"CF-03002".equals(code)) {
            String message = (String) resultInfo.get("message");
            throw new RuntimeException("Codef 계정 등록 실패: " + message);
        }

        return result;
    }

    /**
     * 간편인증 2차 확인 요청
     *
     * <p>사용자가 앱에서 인증을 완료한 후 호출합니다.</p>
     *
     * @return Connected ID를 포함한 응답
     */
    public Map<String, Object> confirmSimpleAuth(Long userId, SimpleAuthRequest request) {
        log.info("[CodefCardService] Confirming simple auth: userId={}, org={}",
                userId, request.getOrganization());

        Map<String, Object> result = codefClient.confirmSimpleAuth(
                request.getOrganization(),
                request.getTwoWayInfo()
        );

        Map<String, Object> resultInfo = extractResult(result);
        String code = (String) resultInfo.get("code");
        if (!"CF-00000".equals(code)) {
            String message = (String) resultInfo.get("message");
            throw new RuntimeException("간편인증 확인 실패: " + message);
        }

        return result;
    }

    /**
     * 기존 Connected ID에 추가 카드사 계정 등록
     */
    public Map<String, Object> addCardAccount(Long userId, String connectedId,
                                               CodefAccountRequest request) {
        log.info("[CodefCardService] Adding card account: userId={}, org={}",
                userId, request.getOrganization());

        Map<String, Object> result = codefClient.addAccount(
                connectedId,
                request.getOrganization(),
                request.getLoginId(),
                request.getLoginPw()
        );

        Map<String, Object> resultInfo = extractResult(result);
        String code = (String) resultInfo.get("code");
        if (!"CF-00000".equals(code)) {
            String message = (String) resultInfo.get("message");
            throw new RuntimeException("Codef 계정 추가 실패: " + message);
        }

        return result;
    }

    /**
     * Connected ID로 보유카드 목록 조회
     *
     * @return 사용자가 선택할 수 있는 카드 목록
     */
    public List<CodefCardInfo> getCardList(Long userId, String connectedId, String organization) {
        log.info("[CodefCardService] Getting card list: userId={}, org={}", userId, organization);

        Map<String, Object> result = codefClient.getCardList(connectedId, organization);

        Map<String, Object> resultInfo = extractResult(result);
        String code = (String) resultInfo.get("code");
        if (!"CF-00000".equals(code)) {
            String message = (String) resultInfo.get("message");
            throw new RuntimeException("보유카드 조회 실패: " + message);
        }

        return parseCardList(result, organization);
    }

    /**
     * 선택한 카드를 가계부에 연동
     */
    @Transactional
    public LinkedCardResponse linkCard(Long userId, LinkCardRequest request) {
        log.info("[CodefCardService] Linking card: userId={}, ledgerId={}, org={}",
                userId, request.getLedgerId(), request.getOrganization());

        // 같은 가계부에 이미 연동된 카드면 skip (기존 정보 반환)
        Optional<LinkedCard> existing = linkedCardRepository
                .findByLedgerIdAndOrganizationAndCardIdAndIsDeletedFalse(
                        request.getLedgerId(), request.getOrganization(), request.getCardId());
        if (existing.isPresent()) {
            log.info("[CodefCardService] Card already linked, skipping: ledgerId={}, cardId={}",
                    request.getLedgerId(), request.getCardId());
            return LinkedCardResponse.from(existing.get());
        }

        LinkedCard linkedCard = LinkedCard.builder()
                .userId(userId)
                .ledgerId(request.getLedgerId())
                .connectedId(request.getConnectedId())
                .organization(request.getOrganization())
                .cardName(request.getCardName())
                .cardNoMasked(request.getCardNoMasked())
                .cardId(request.getCardId())
                .build();

        LinkedCard saved = linkedCardRepository.save(linkedCard);
        log.info("[CodefCardService] Card linked: linkedCardId={}", saved.getLinkedCardId());

        return LinkedCardResponse.from(saved);
    }

    /**
     * 내 연동 카드 목록 조회
     */
    @Transactional(readOnly = true)
    public List<LinkedCardResponse> getLinkedCards(Long userId) {
        List<LinkedCard> cards = linkedCardRepository.findByUserIdAndIsDeletedFalse(userId);
        return cards.stream()
                .map(LinkedCardResponse::from)
                .toList();
    }

    /**
     * 특정 가계부의 연동 카드 목록 조회
     */
    @Transactional(readOnly = true)
    public List<LinkedCardResponse> getLinkedCardsByLedger(Long userId, Long ledgerId) {
        List<LinkedCard> cards = linkedCardRepository
                .findByUserIdAndLedgerIdAndIsDeletedFalse(userId, ledgerId);
        return cards.stream()
                .map(LinkedCardResponse::from)
                .toList();
    }

    /**
     * 카드 연동 해제 (soft delete)
     *
     * <p>연동 카드와 해당 카드의 CODEF 거래 내역을 모두 soft delete합니다.</p>
     * <p>30일 후 스케줄러에 의해 실제 삭제됩니다.</p>
     */
    @Transactional
    public void unlinkCard(Long userId, Long linkedCardId) {
        log.info("[CodefCardService] Unlinking card: userId={}, linkedCardId={}", userId, linkedCardId);

        LinkedCard card = linkedCardRepository.findByLinkedCardIdAndUserIdAndIsDeletedFalse(linkedCardId, userId)
                .orElseThrow(() -> new RuntimeException("연동 카드를 찾을 수 없습니다."));

        // 1. 연동 카드 soft delete
        card.delete();
        linkedCardRepository.save(card);

        // 2. 해당 카드의 CODEF 거래 내역 일괄 soft delete
        int deletedCount = transactionRepository.softDeleteByLinkedCardId(linkedCardId);
        log.info("[CodefCardService] Card unlinked: linkedCardId={}, deletedTransactions={}",
                linkedCardId, deletedCount);
    }

    /**
     * Codef 응답에서 result 객체 추출
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(Map<String, Object> response) {
        Object result = response.get("result");
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        throw new RuntimeException("Codef 응답에서 result를 찾을 수 없습니다.");
    }

    /**
     * Codef 보유카드 응답을 CodefCardInfo 리스트로 변환
     */
    @SuppressWarnings("unchecked")
    private List<CodefCardInfo> parseCardList(Map<String, Object> response, String organization) {
        List<CodefCardInfo> cardInfoList = new ArrayList<>();

        Object data = response.get("data");
        if (data instanceof List) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
            for (Map<String, Object> cardData : dataList) {
                String cardNo = getStringValue(cardData, "resCardNo");
                String maskedNo = cardNo != null && cardNo.length() >= 4
                        ? cardNo.substring(cardNo.length() - 4) : "";

                cardInfoList.add(CodefCardInfo.builder()
                        .cardId(getStringValue(cardData, "resCardNo"))
                        .cardName(getStringValue(cardData, "resCardName"))
                        .cardNoMasked(maskedNo)
                        .organization(organization)
                        .build());
            }
        }

        return cardInfoList;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
