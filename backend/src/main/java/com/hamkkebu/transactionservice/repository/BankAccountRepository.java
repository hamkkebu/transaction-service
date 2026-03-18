package com.hamkkebu.transactionservice.repository;

import com.hamkkebu.transactionservice.data.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * 계정의 활성 은행 계좌 목록 조회
     */
    List<BankAccount> findByAccountIdAndIsDeletedFalse(Long accountId);

    /**
     * 특정 은행 계좌 조회 (활성 상태)
     */
    Optional<BankAccount> findByBankAccountIdAndIsDeletedFalse(Long bankAccountId);
}
