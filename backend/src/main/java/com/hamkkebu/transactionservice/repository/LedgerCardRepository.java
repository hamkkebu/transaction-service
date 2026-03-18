package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.transactionservice.data.entity.LedgerCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerCardRepository extends JpaRepository<LedgerCard, Long> {

    /**
     * 특정 가계부의 활성 카드 목록 조회
     */
    List<LedgerCard> findByLedgerIdAndIsDeletedFalse(Long ledgerId);

    /**
     * 특정 카드가 등록된 활성 가계부-카드 목록 조회
     */
    List<LedgerCard> findByCardIdAndIsDeletedFalse(Long cardId);

    /**
     * 특정 가계부 + 카드 조합 조회
     */
    Optional<LedgerCard> findByLedgerIdAndCardIdAndIsDeletedFalse(Long ledgerId, Long cardId);

    /**
     * 특정 가계부 + 카드 조합 존재 여부 확인
     */
    boolean existsByLedgerIdAndCardIdAndIsDeletedFalse(Long ledgerId, Long cardId);

    /**
     * 특정 사용자가 연결한 가계부-카드 목록 조회
     */
    List<LedgerCard> findByLinkedByAndIsDeletedFalse(Long linkedBy);
}
