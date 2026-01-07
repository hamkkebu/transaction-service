package com.hamkkebu.transactionservice.service;

import com.hamkkebu.boilerplate.common.exception.BusinessException;
import com.hamkkebu.boilerplate.common.exception.ErrorCode;
import com.hamkkebu.boilerplate.common.util.BigDecimalUtils;
import com.hamkkebu.transactionservice.data.dto.PeriodTransactionSummary;
import com.hamkkebu.transactionservice.data.dto.PeriodTransactionSummary.PeriodDetail;
import com.hamkkebu.transactionservice.data.dto.PeriodTransactionSummary.PeriodType;
import com.hamkkebu.transactionservice.data.dto.TransactionRequest;
import com.hamkkebu.transactionservice.data.dto.TransactionResponse;
import com.hamkkebu.transactionservice.data.dto.TransactionSummary;
import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import com.hamkkebu.transactionservice.data.mapper.TransactionMapper;
import com.hamkkebu.transactionservice.kafka.producer.TransactionEventProducer;
import com.hamkkebu.transactionservice.repository.LedgerRepository;
import com.hamkkebu.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionEventProducer transactionEventProducer;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        log.info("Creating transaction for ledger {} by user {}", request.getLedgerId(), userId);

        // 가계부 소유권 검증
        validateLedgerOwnership(request.getLedgerId(), userId);

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setUserId(userId);

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created transaction with id {}", savedTransaction.getId());

        // Kafka 이벤트 발행
        transactionEventProducer.publishTransactionCreated(savedTransaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id, Long userId) {
        Transaction transaction = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByLedger(Long ledgerId, Long userId, Pageable pageable) {
        log.debug("Fetching transactions for ledger {} by user {}", ledgerId, userId);
        validateLedgerAccess(ledgerId, userId);
        Page<Transaction> transactions = transactionRepository
                .findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId, pageable);
        return transactions.map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactionsByLedger(Long ledgerId, Long userId) {
        log.debug("Fetching all transactions for ledger {} by user {}", ledgerId, userId);
        validateLedgerAccess(ledgerId, userId);
        List<Transaction> transactions = transactionRepository
                .findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId);
        return transactionMapper.toResponseList(transactions);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request, Long userId) {
        log.debug("Updating transaction {} by user {}", id, userId);

        Transaction transaction = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        transactionMapper.updateEntity(request, transaction);
        Transaction updatedTransaction = transactionRepository.save(transaction);

        // Kafka 이벤트 발행
        transactionEventProducer.publishTransactionUpdated(updatedTransaction);

        log.info("Updated transaction {}", id);
        return transactionMapper.toResponse(updatedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        log.debug("Deleting transaction {} by user {}", id, userId);

        Transaction transaction = transactionRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        transaction.delete();
        transactionRepository.save(transaction);

        // Kafka 이벤트 발행
        transactionEventProducer.publishTransactionDeleted(transaction);

        log.info("Deleted transaction {}", id);
    }

    @Transactional(readOnly = true)
    public TransactionSummary getSummaryByLedger(Long ledgerId, Long userId) {
        log.debug("Calculating summary for ledger {} by user {}", ledgerId, userId);
        validateLedgerAccess(ledgerId, userId);

        BigDecimal totalIncome = BigDecimalUtils.nullToZero(
                transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.INCOME));
        BigDecimal totalExpense = BigDecimalUtils.nullToZero(
                transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.EXPENSE));
        Long transactionCount = transactionRepository.countByLedgerId(ledgerId);

        BigDecimal balance = BigDecimalUtils.calculateBalance(totalIncome, totalExpense);

        return TransactionSummary.builder()
                .ledgerId(ledgerId)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .transactionCount(transactionCount)
                .build();
    }

    /**
     * 일별 거래 요약 조회
     * @param ledgerId 가계부 ID
     * @param date 조회할 날짜
     * @param userId 사용자 ID
     * @return 일별 거래 요약
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getDailySummary(Long ledgerId, LocalDate date, Long userId) {
        log.debug("Fetching daily summary for ledger {} on {} by user {}", ledgerId, date, userId);
        validateLedgerAccess(ledgerId, userId);

        return buildPeriodSummary(ledgerId, PeriodType.DAILY, date, date);
    }

    /**
     * 월별 거래 요약 조회
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getMonthlySummary(Long ledgerId, int year, int month, Long userId) {
        log.debug("Fetching monthly summary for ledger {} on {}-{} by user {}", ledgerId, year, month, userId);
        validateLedgerAccess(ledgerId, userId);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        PeriodTransactionSummary summary = buildPeriodSummary(ledgerId, PeriodType.MONTHLY, startDate, endDate);
        summary.setPeriodDetails(buildDailyDetails(ledgerId, startDate, endDate));

        return summary;
    }

    /**
     * 년별 거래 요약 조회
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getYearlySummary(Long ledgerId, int year, Long userId) {
        log.debug("Fetching yearly summary for ledger {} on {} by user {}", ledgerId, year, userId);
        validateLedgerAccess(ledgerId, userId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        PeriodTransactionSummary summary = buildPeriodSummary(ledgerId, PeriodType.YEARLY, startDate, endDate);
        summary.setPeriodDetails(buildMonthlyDetails(ledgerId, year));

        return summary;
    }

    /**
     * 기간별 거래 요약 조회 (커스텀 기간)
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getPeriodSummary(Long ledgerId, LocalDate startDate, LocalDate endDate, Long userId) {
        log.debug("Fetching period summary for ledger {} from {} to {} by user {}", ledgerId, startDate, endDate, userId);
        validateLedgerAccess(ledgerId, userId);

        return buildPeriodSummary(ledgerId, PeriodType.DAILY, startDate, endDate);
    }

    /**
     * 기간별 거래 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByPeriod(Long ledgerId, LocalDate startDate, LocalDate endDate, Long userId) {
        log.debug("Fetching transactions for ledger {} from {} to {} by user {}", ledgerId, startDate, endDate, userId);
        validateLedgerAccess(ledgerId, userId);

        List<Transaction> transactions = transactionRepository
                .findByLedgerIdAndTransactionDateBetweenAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
                        ledgerId, startDate, endDate);
        return transactionMapper.toResponseList(transactions);
    }

    /**
     * 기간별 요약 빌드 헬퍼 메서드
     */
    private PeriodTransactionSummary buildPeriodSummary(Long ledgerId, PeriodType periodType,
                                                        LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = BigDecimalUtils.nullToZero(
                transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                        ledgerId, TransactionType.INCOME, startDate, endDate));
        BigDecimal totalExpense = BigDecimalUtils.nullToZero(
                transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                        ledgerId, TransactionType.EXPENSE, startDate, endDate));
        Long transactionCount = transactionRepository.countByLedgerIdAndDateRange(ledgerId, startDate, endDate);

        BigDecimal balance = BigDecimalUtils.calculateBalance(totalIncome, totalExpense);

        List<Transaction> transactions = transactionRepository
                .findByLedgerIdAndTransactionDateBetweenAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
                        ledgerId, startDate, endDate);

        return PeriodTransactionSummary.builder()
                .ledgerId(ledgerId)
                .periodType(periodType)
                .startDate(startDate)
                .endDate(endDate)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .transactionCount(transactionCount)
                .transactions(transactionMapper.toResponseList(transactions))
                .build();
    }

    /**
     * 일별 상세 요약 빌드 (월별 조회 시 사용)
     */
    private List<PeriodDetail> buildDailyDetails(Long ledgerId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions = transactionRepository
                .findByLedgerIdAndTransactionDateBetweenAndIsDeletedFalseOrderByTransactionDateDescIdDesc(
                        ledgerId, startDate, endDate);

        Map<LocalDate, List<Transaction>> groupedByDate = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        return groupedByDate.entrySet().stream()
                .map(entry -> buildPeriodDetailFromTransactions(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .toList();
    }

    /**
     * 거래 목록으로부터 PeriodDetail 생성
     */
    private PeriodDetail buildPeriodDetailFromTransactions(LocalDate date, List<Transaction> transactions) {
        BigDecimal income = calculateAmountByType(transactions, TransactionType.INCOME);
        BigDecimal expense = calculateAmountByType(transactions, TransactionType.EXPENSE);

        return PeriodDetail.builder()
                .periodLabel(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .startDate(date)
                .endDate(date)
                .income(income)
                .expense(expense)
                .balance(BigDecimalUtils.calculateBalance(income, expense))
                .transactionCount((long) transactions.size())
                .build();
    }

    /**
     * 거래 유형별 금액 합계 계산
     */
    private BigDecimal calculateAmountByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 월별 상세 요약 빌드 (년별 조회 시 사용)
     */
    private List<PeriodDetail> buildMonthlyDetails(Long ledgerId, int year) {
        List<PeriodDetail> details = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            BigDecimal income = BigDecimalUtils.nullToZero(
                    transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                            ledgerId, TransactionType.INCOME, monthStart, monthEnd));
            BigDecimal expense = BigDecimalUtils.nullToZero(
                    transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                            ledgerId, TransactionType.EXPENSE, monthStart, monthEnd));
            Long count = transactionRepository.countByLedgerIdAndDateRange(ledgerId, monthStart, monthEnd);

            // 거래가 있는 월만 포함
            if (count > 0) {
                details.add(PeriodDetail.builder()
                        .periodLabel(yearMonth.format(formatter))
                        .startDate(monthStart)
                        .endDate(monthEnd)
                        .income(income)
                        .expense(expense)
                        .balance(BigDecimalUtils.calculateBalance(income, expense))
                        .transactionCount(count)
                        .build());
            }
        }

        return details;
    }

    /**
     * 가계부 소유권 검증 (동기화된 가계부 정보로 검증)
     *
     * <p>ledger-service에서 Kafka를 통해 동기화된 가계부 정보를 조회하여
     * 해당 사용자의 가계부인지 확인합니다.</p>
     *
     * @param ledgerId 가계부 ID
     * @param userId   사용자 ID
     * @throws BusinessException 가계부가 없거나 접근 권한이 없는 경우
     */
    private void validateLedgerOwnership(Long ledgerId, Long userId) {
        // 동기화된 가계부 정보로 소유권 검증
        if (!ledgerRepository.existsByLedgerIdAndUserIdAndIsDeletedFalse(ledgerId, userId)) {
            // 가계부가 존재하지 않거나 다른 사용자의 가계부
            if (!ledgerRepository.existsByLedgerIdAndIsDeletedFalse(ledgerId)) {
                log.warn("Ledger {} not found", ledgerId);
                throw new BusinessException(ErrorCode.LEDGER_NOT_FOUND);
            }
            log.warn("User {} attempted to access ledger {} without permission", userId, ledgerId);
            throw new BusinessException(ErrorCode.LEDGER_ACCESS_DENIED);
        }
    }

    /**
     * 가계부 접근 권한 검증 (기존 거래 목록 조회용)
     *
     * <p>해당 가계부에 사용자의 거래가 있거나, 동기화된 가계부 정보로 소유권이 확인되면 접근 허용</p>
     */
    private void validateLedgerAccess(Long ledgerId, Long userId) {
        // 먼저 동기화된 가계부 정보로 소유권 확인
        if (ledgerRepository.existsByLedgerIdAndUserIdAndIsDeletedFalse(ledgerId, userId)) {
            return; // 소유권 확인됨
        }

        // 동기화된 가계부 정보가 없는 경우 (아직 동기화되지 않은 경우)
        // 기존 로직: 해당 가계부에 해당 사용자의 거래가 있는지 확인
        boolean hasAccess = transactionRepository.existsByLedgerIdAndUserIdAndIsDeletedFalse(ledgerId, userId);
        if (!hasAccess) {
            Long totalCount = transactionRepository.countByLedgerId(ledgerId);
            if (totalCount > 0) {
                // 다른 사용자의 거래가 있으면 접근 거부
                log.warn("User {} attempted to access ledger {} without permission", userId, ledgerId);
                throw new BusinessException(ErrorCode.LEDGER_ACCESS_DENIED);
            }
            // 거래가 없고 가계부 정보도 없으면 가계부 미존재
            if (!ledgerRepository.existsByLedgerIdAndIsDeletedFalse(ledgerId)) {
                log.warn("Ledger {} not found or not synced yet", ledgerId);
                throw new BusinessException(ErrorCode.LEDGER_NOT_FOUND);
            }
        }
    }
}
