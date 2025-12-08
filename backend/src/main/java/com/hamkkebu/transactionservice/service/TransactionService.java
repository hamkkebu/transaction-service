package com.hamkkebu.transactionservice.service;

import com.hamkkebu.boilerplate.common.exception.BusinessException;
import com.hamkkebu.boilerplate.common.exception.ErrorCode;
import com.hamkkebu.transactionservice.data.dto.TransactionRequest;
import com.hamkkebu.transactionservice.data.dto.TransactionResponse;
import com.hamkkebu.transactionservice.data.dto.TransactionSummary;
import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import com.hamkkebu.transactionservice.data.mapper.TransactionMapper;
import com.hamkkebu.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        log.info("Creating transaction for ledger {} by user {}", request.getLedgerId(), userId);

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUserId(userId);

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created transaction with id {}", savedTransaction.getId());

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        Transaction transaction = transactionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));
        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByLedger(Long ledgerId, Pageable pageable) {
        log.info("Fetching transactions for ledger {}", ledgerId);
        Page<Transaction> transactions = transactionRepository
                .findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId, pageable);
        return transactions.map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactionsByLedger(Long ledgerId) {
        log.info("Fetching all transactions for ledger {}", ledgerId);
        List<Transaction> transactions = transactionRepository
                .findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId);
        return transactionMapper.toResponseList(transactions);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        log.info("Updating transaction {}", id);

        Transaction transaction = transactionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        transactionMapper.updateEntity(request, transaction);
        Transaction updatedTransaction = transactionRepository.save(transaction);

        log.info("Updated transaction {}", id);
        return transactionMapper.toResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        log.info("Deleting transaction {}", id);

        Transaction transaction = transactionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        transaction.delete();
        transactionRepository.save(transaction);

        log.info("Deleted transaction {}", id);
    }

    @Transactional(readOnly = true)
    public TransactionSummary getSummaryByLedger(Long ledgerId) {
        log.info("Calculating summary for ledger {}", ledgerId);

        BigDecimal totalIncome = transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.EXPENSE);
        Long transactionCount = transactionRepository.countByLedgerId(ledgerId);

        BigDecimal balance = totalIncome.subtract(totalExpense);

        return TransactionSummary.builder()
                .ledgerId(ledgerId)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .transactionCount(transactionCount)
                .build();
    }
}
