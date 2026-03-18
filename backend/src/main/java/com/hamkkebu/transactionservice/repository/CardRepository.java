package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.transactionservice.data.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * 계정의 활성 카드 목록 조회
     */
    List<Card> findByAccountIdAndIsDeletedFalse(Long accountId);

    /**
     * Connected ID + 카드사 + 카드 식별자로 카드 조회 (중복 체크용)
     */
    Optional<Card> findByConnectedIdAndOrganizationAndCardIdentifierAndIsDeletedFalse(
            String connectedId, String organization, String cardIdentifier);

    /**
     * 특정 카드 조회 (활성 상태)
     */
    Optional<Card> findByCardIdAndIsDeletedFalse(Long cardId);
}
