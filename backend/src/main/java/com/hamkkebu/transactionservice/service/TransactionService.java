package com.hamkkebu.transactionservice.service;

import com.hamkkebu.boilerplate.common.exception.BusinessException;
import com.hamkkebu.boilerplate.common.exception.ErrorCode;
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
    private final TransactionMapper transactionMapper;
    private final TransactionEventProducer transactionEventProducer;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, Long userId) {
        log.info("Creating transaction for ledger {} by user {}", request.getLedgerId(), userId);

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
        log.info("Fetching transactions for ledger {} by user {}", ledgerId, userId);
        validateLedgerAccess(ledgerId, userId);
        Page<Transaction> transactions = transactionRepository
                .findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId, pageable);
        return transactions.map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactionsByLedger(Long ledgerId, Long userId) {
        log.info("Fetching all transactions for ledger {} by user {}", ledgerId, userId);
        validateLedgerAccess(ledgerId, userId);
        List<Transaction> transactions = transactionRepository
                .findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId);
        return transactionMapper.toResponseList(transactions);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request, Long userId) {
        log.info("Updating transaction {} by user {}", id, userId);

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
        log.info("Deleting transaction {} by user {}", id, userId);

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
        log.info("Calculating summary for ledger {} by user {}", ledgerId, userId);
        validateLedgerAccess(ledgerId, userId);

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

    /**
     * 일별 거래 요약 조회
     * @param ledgerId 가계부 ID
     * @param date 조회할 날짜
     * @param userId 사용자 ID
     * @return 일별 거래 요약
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getDailySummary(Long ledgerId, LocalDate date, Long userId) {
        log.info("Fetching daily summary for ledger {} on {} by user {}", ledgerId, date, userId);
        validateLedgerAccess(ledgerId, userId);

        LocalDate startDate = date;
        LocalDate endDate = date;

        return buildPeriodSummary(ledgerId, PeriodType.DAILY, startDate, endDate);
    }

    /**
     * 월별 거래 요약 조회
     * @param ledgerId 가계부 ID
     * @param year 연도
     * @param month 월
     * @param userId 사용자 ID
     * @return 월별 거래 요약 (일별 상세 포함)
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getMonthlySummary(Long ledgerId, int year, int month, Long userId) {
        log.info("Fetching monthly summary for ledger {} on {}-{} by user {}", ledgerId, year, month, userId);
        validateLedgerAccess(ledgerId, userId);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        PeriodTransactionSummary summary = buildPeriodSummary(ledgerId, PeriodType.MONTHLY, startDate, endDate);

        // 일별 상세 요약 추가
        List<PeriodDetail> dailyDetails = buildDailyDetails(ledgerId, startDate, endDate);
        summary.setPeriodDetails(dailyDetails);

        return summary;
    }

    /**
     * 년별 거래 요약 조회
     * @param ledgerId 가계부 ID
     * @param year 연도
     * @param userId 사용자 ID
     * @return 년별 거래 요약 (월별 상세 포함)
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getYearlySummary(Long ledgerId, int year, Long userId) {
        log.info("Fetching yearly summary for ledger {} on {} by user {}", ledgerId, year, userId);
        validateLedgerAccess(ledgerId, userId);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        PeriodTransactionSummary summary = buildPeriodSummary(ledgerId, PeriodType.YEARLY, startDate, endDate);

        // 월별 상세 요약 추가
        List<PeriodDetail> monthlyDetails = buildMonthlyDetails(ledgerId, year);
        summary.setPeriodDetails(monthlyDetails);

        return summary;
    }

    /**
     * 기간별 거래 요약 조회 (커스텀 기간)
     * @param ledgerId 가계부 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @param userId 사용자 ID
     * @return 기간별 거래 요약
     */
    @Transactional(readOnly = true)
    public PeriodTransactionSummary getPeriodSummary(Long ledgerId, LocalDate startDate, LocalDate endDate, Long userId) {
        log.info("Fetching period summary for ledger {} from {} to {} by user {}", ledgerId, startDate, endDate, userId);
        validateLedgerAccess(ledgerId, userId);

        return buildPeriodSummary(ledgerId, PeriodType.DAILY, startDate, endDate);
    }

    /**
     * 기간별 거래 목록 조회
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByPeriod(Long ledgerId, LocalDate startDate, LocalDate endDate, Long userId) {
        log.info("Fetching transactions for ledger {} from {} to {} by user {}", ledgerId, startDate, endDate, userId);
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
        BigDecimal totalIncome = transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                ledgerId, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                ledgerId, TransactionType.EXPENSE, startDate, endDate);
        Long transactionCount = transactionRepository.countByLedgerIdAndDateRange(ledgerId, startDate, endDate);

        BigDecimal balance = totalIncome.subtract(totalExpense);

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

        // 날짜별로 그룹핑
        Map<LocalDate, List<Transaction>> groupedByDate = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        List<PeriodDetail> details = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 시작일부터 종료일까지 모든 날짜에 대해 요약 생성
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Transaction> dayTransactions = groupedByDate.getOrDefault(date, List.of());

            BigDecimal income = dayTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal expense = dayTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 거래가 있는 날만 포함
            if (!dayTransactions.isEmpty()) {
                details.add(PeriodDetail.builder()
                        .periodLabel(date.format(formatter))
                        .startDate(date)
                        .endDate(date)
                        .income(income)
                        .expense(expense)
                        .balance(income.subtract(expense))
                        .transactionCount((long) dayTransactions.size())
                        .build());
            }
        }

        return details;
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

            BigDecimal income = transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                    ledgerId, TransactionType.INCOME, monthStart, monthEnd);
            BigDecimal expense = transactionRepository.sumAmountByLedgerIdAndTypeAndDateRange(
                    ledgerId, TransactionType.EXPENSE, monthStart, monthEnd);
            Long count = transactionRepository.countByLedgerIdAndDateRange(ledgerId, monthStart, monthEnd);

            // 거래가 있는 월만 포함
            if (count > 0) {
                details.add(PeriodDetail.builder()
                        .periodLabel(yearMonth.format(formatter))
                        .startDate(monthStart)
                        .endDate(monthEnd)
                        .income(income)
                        .expense(expense)
                        .balance(income.subtract(expense))
                        .transactionCount(count)
                        .build());
            }
        }

        return details;
    }

    /**
     * 가계부 접근 권한 검증 (해당 가계부에 거래가 있는 사용자인지 확인)
     * 가계부에 해당 사용자의 거래가 있거나, 새로운 거래 생성 시에는 허용
     */
    private void validateLedgerAccess(Long ledgerId, Long userId) {
        // 해당 가계부에 해당 사용자의 거래가 있는지 확인
        boolean hasAccess = transactionRepository.existsByLedgerIdAndUserIdAndIsDeletedFalse(ledgerId, userId);
        // 아직 거래가 없는 경우에도 접근 허용 (새 가계부의 경우)
        // 실제로는 ledger-service와 gRPC 통신으로 가계부 소유권을 검증해야 함
        // 현재는 거래가 존재하면 그 사용자의 것인지만 확인
        if (!hasAccess) {
            // 거래가 없는 경우는 허용 (새로운 가계부에 거래 추가 가능)
            Long existingCount = transactionRepository.countByLedgerIdAndUserId(ledgerId, userId);
            Long totalCount = transactionRepository.countByLedgerId(ledgerId);
            // 다른 사용자의 거래가 있으면 접근 거부
            if (totalCount > 0 && existingCount == 0) {
                log.warn("User {} attempted to access ledger {} without permission", userId, ledgerId);
                throw new BusinessException(ErrorCode.LEDGER_ACCESS_DENIED);
            }
        }
    }
}
