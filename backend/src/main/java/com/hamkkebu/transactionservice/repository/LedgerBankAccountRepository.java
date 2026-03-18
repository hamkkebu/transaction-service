package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.transactionservice.data.entity.LedgerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerBankAccountRepository extends JpaRepository<LedgerBankAccount, Long> {

    /**
     * 특정 가계부의 활성 은행 계좌 목록 조회
     */
    List<LedgerBankAccount> findByLedgerIdAndIsDeletedFalse(Long ledgerId);

    /**
     * 특정 은행 계좌가 등록된 활성 가계부-계좌 목록 조회
     */
    List<LedgerBankAccount> findByBankAccountIdAndIsDeletedFalse(Long bankAccountId);

    /**
     * 특정 가계부 + 은행 계좌 조합 존재 여부 확인
     */
    boolean existsByLedgerIdAndBankAccountIdAndIsDeletedFalse(Long ledgerId, Long bankAccountId);
}
