package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 특정 가계부의 모든 거래 조회 (삭제되지 않은 것만)
    Page<Transaction> findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
            Long ledgerId, Pageable pageable);

    List<Transaction> findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(Long ledgerId);

    // 특정 거래 조회 (삭제되지 않은 것만)
    Optional<Transaction> findByIdAndIsDeletedFalse(Long id);

    // 특정 가계부의 총 수입 계산
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.ledgerId = :ledgerId AND t.type = :type AND t.isDeleted = false")
    BigDecimal sumAmountByLedgerIdAndType(@Param("ledgerId") Long ledgerId, @Param("type") TransactionType type);

    // 특정 가계부의 거래 수
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.ledgerId = :ledgerId AND t.isDeleted = false")
    Long countByLedgerId(@Param("ledgerId") Long ledgerId);

    // 기간별 거래 조회 (페이징)
    Page<Transaction> findByLedgerIdAndTransactionDateBetweenAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
            Long ledgerId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // 기간별 거래 조회 (전체)
    List<Transaction> findByLedgerIdAndTransactionDateBetweenAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
            Long ledgerId, LocalDate startDate, LocalDate endDate);

    // 카테고리별 거래 조회
    Page<Transaction> findByLedgerIdAndCategoryAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
            Long ledgerId, String category, Pageable pageable);

    // 기간별 총 수입 계산
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.ledgerId = :ledgerId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false")
    BigDecimal sumAmountByLedgerIdAndTypeAndDateRange(
            @Param("ledgerId") Long ledgerId,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 기간별 거래 수 계산
    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.ledgerId = :ledgerId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isDeleted = false")
    Long countByLedgerIdAndDateRange(
            @Param("ledgerId") Long ledgerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
