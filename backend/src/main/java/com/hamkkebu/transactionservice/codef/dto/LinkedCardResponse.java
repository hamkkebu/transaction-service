package com.hamkkebu.transactionservice.codef.dto;

import com.hamkkebu.transactionservice.data.entity.LinkedCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 연동 카드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedCardResponse {

    private Long linkedCardId;
    private Long ledgerId;
    private String organization;
    private String organizationName;
    private String cardId;
    private String cardName;
    private String cardNoMasked;
    private String lastSyncedDate;
    private LocalDateTime createdAt;

    public static LinkedCardResponse from(LinkedCard card) {
        return LinkedCardResponse.builder()
                .linkedCardId(card.getLinkedCardId())
                .ledgerId(card.getLedgerId())
                .organization(card.getOrganization())
                .organizationName(resolveOrganizationName(card.getOrganization()))
                .cardId(card.getCardId())
                .cardName(card.getCardName())
                .cardNoMasked(card.getCardNoMasked())
                .lastSyncedDate(card.getLastSyncedDate())
                .createdAt(card.getCreatedAt())
                .build();
    }

    /**
     * 카드사 기관 코드 → 카드사명 변환
     */
    private static String resolveOrganizationName(String organization) {
        return switch (organization) {
            case "0301" -> "KB국민카드";
            case "0302" -> "현대카드";
            case "0303" -> "삼성카드";
            case "0304" -> "NH농협카드";
            case "0305" -> "BC카드";
            case "0306" -> "신한카드";
            case "0307" -> "씨티카드";
            case "0309" -> "우리카드";
            case "0310" -> "하나카드";
            case "0311" -> "롯데카드";
            case "0312" -> "전북카드";
            case "0313" -> "광주카드";
            case "0314" -> "수협카드";
            case "0315" -> "제주카드";
            default -> organization;
        };
    }
}
