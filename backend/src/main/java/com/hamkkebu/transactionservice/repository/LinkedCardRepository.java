package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.transactionservice.data.entity.LinkedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedCardRepository extends JpaRepository<LinkedCard, Long> {

    /**
     * 사용자의 활성 연동 카드 목록 조회
     */
    List<LinkedCard> findByUserIdAndIsDeletedFalse(Long userId);

    /**
     * 특정 가계부의 활성 연동 카드 목록 조회
     */
    List<LinkedCard> findByUserIdAndLedgerIdAndIsDeletedFalse(Long userId, Long ledgerId);

    /**
     * 특정 연동 카드 조회 (활성 상태)
     */
    Optional<LinkedCard> findByLinkedCardIdAndUserIdAndIsDeletedFalse(Long linkedCardId, Long userId);

    /**
     * 삭제 후 유예기간(30일) 지난 카드 조회 (hard delete 대상)
     */
    List<LinkedCard> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoffDate);

    /**
     * Connected ID로 카드 존재 여부 확인
     */
    boolean existsByConnectedIdAndOrganizationAndCardIdAndIsDeletedFalse(
            String connectedId, String organization, String cardId);

    /**
     * 특정 가계부 + 카드사 + 카드ID로 연동 카드 조회 (중복 체크용)
     */
    Optional<LinkedCard> findByLedgerIdAndOrganizationAndCardIdAndIsDeletedFalse(
            Long ledgerId, String organization, String cardId);
}
