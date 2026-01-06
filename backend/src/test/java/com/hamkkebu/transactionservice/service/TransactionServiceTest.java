package com.hamkkebu.transactionservice.service;

import com.hamkkebu.boilerplate.common.exception.BusinessException;
import com.hamkkebu.boilerplate.common.exception.ErrorCode;
import com.hamkkebu.transactionservice.data.dto.TransactionRequest;
import com.hamkkebu.transactionservice.data.dto.TransactionResponse;
import com.hamkkebu.transactionservice.data.dto.TransactionSummary;
import com.hamkkebu.transactionservice.data.entity.Transaction;
import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import com.hamkkebu.transactionservice.data.mapper.TransactionMapper;
import com.hamkkebu.transactionservice.kafka.producer.TransactionEventProducer;
import com.hamkkebu.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService 테스트")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionRequest validRequest;
    private Transaction savedTransaction;
    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        validRequest = TransactionRequest.builder()
                .ledgerId(1L)
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.valueOf(50000))
                .description("점심 식사")
                .category("식비")
                .transactionDate(LocalDate.now())
                .memo("팀 점심")
                .build();

        savedTransaction = Transaction.builder()
                .ledgerId(1L)
                .userId(1L)
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.valueOf(50000))
                .description("점심 식사")
                .category("식비")
                .transactionDate(LocalDate.now())
                .memo("팀 점심")
                .build();
        ReflectionTestUtils.setField(savedTransaction, "id", 1L);

        transactionResponse = TransactionResponse.builder()
                .id(1L)
                .ledgerId(1L)
                .userId(1L)
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.valueOf(50000))
                .description("점심 식사")
                .category("식비")
                .transactionDate(LocalDate.now())
                .memo("팀 점심")
                .build();
    }

    @Test
    @DisplayName("거래 생성 성공")
    void createTransaction_Success() {
        // Given
        Long userId = 1L;
        when(transactionMapper.toEntity(validRequest)).thenReturn(savedTransaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(transactionResponse);
        doNothing().when(transactionEventProducer).publishTransactionCreated(any(Transaction.class));

        // When
        TransactionResponse result = transactionService.createTransaction(validRequest, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLedgerId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(result.getDescription()).isEqualTo("점심 식사");

        verify(transactionMapper).toEntity(validRequest);
        verify(transactionRepository).save(any(Transaction.class));
        verify(transactionEventProducer).publishTransactionCreated(any(Transaction.class));
        verify(transactionMapper).toResponse(savedTransaction);
    }

    @Test
    @DisplayName("거래 조회 성공")
    void getTransaction_Success() {
        // Given
        when(transactionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(savedTransaction));
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(transactionResponse);

        // When
        TransactionResponse result = transactionService.getTransaction(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(transactionRepository).findByIdAndIsDeletedFalse(1L);
        verify(transactionMapper).toResponse(savedTransaction);
    }

    @Test
    @DisplayName("거래 조회 실패 - 존재하지 않는 거래")
    void getTransaction_NotFound() {
        // Given
        when(transactionRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransaction(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND);

        verify(transactionRepository).findByIdAndIsDeletedFalse(999L);
        verify(transactionMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("가계부별 거래 목록 조회 성공")
    void getTransactionsByLedger_Success() {
        // Given
        Long ledgerId = 1L;
        Pageable pageable = PageRequest.of(0, 20);
        List<Transaction> transactions = List.of(savedTransaction);
        Page<Transaction> transactionPage = new PageImpl<>(transactions, pageable, 1);

        when(transactionRepository.findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId, pageable))
                .thenReturn(transactionPage);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(transactionResponse);

        // When
        Page<TransactionResponse> result = transactionService.getTransactionsByLedger(ledgerId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);

        verify(transactionRepository).findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId, pageable);
    }

    @Test
    @DisplayName("거래 수정 성공")
    void updateTransaction_Success() {
        // Given
        TransactionRequest updateRequest = TransactionRequest.builder()
                .ledgerId(1L)
                .type(TransactionType.EXPENSE)
                .amount(BigDecimal.valueOf(60000))
                .description("저녁 식사")
                .category("식비")
                .transactionDate(LocalDate.now())
                .memo("회식")
                .build();

        when(transactionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(savedTransaction));
        doNothing().when(transactionMapper).updateEntity(updateRequest, savedTransaction);
        when(transactionRepository.save(savedTransaction)).thenReturn(savedTransaction);
        when(transactionMapper.toResponse(savedTransaction)).thenReturn(transactionResponse);
        doNothing().when(transactionEventProducer).publishTransactionUpdated(any(Transaction.class));

        // When
        TransactionResponse result = transactionService.updateTransaction(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();

        verify(transactionRepository).findByIdAndIsDeletedFalse(1L);
        verify(transactionMapper).updateEntity(updateRequest, savedTransaction);
        verify(transactionRepository).save(savedTransaction);
        verify(transactionEventProducer).publishTransactionUpdated(savedTransaction);
    }

    @Test
    @DisplayName("거래 수정 실패 - 존재하지 않는 거래")
    void updateTransaction_NotFound() {
        // Given
        when(transactionRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.updateTransaction(999L, validRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND);

        verify(transactionRepository).findByIdAndIsDeletedFalse(999L);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("거래 삭제 성공")
    void deleteTransaction_Success() {
        // Given
        when(transactionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(savedTransaction));
        when(transactionRepository.save(savedTransaction)).thenReturn(savedTransaction);
        doNothing().when(transactionEventProducer).publishTransactionDeleted(any(Transaction.class));

        // When
        transactionService.deleteTransaction(1L);

        // Then
        verify(transactionRepository).findByIdAndIsDeletedFalse(1L);
        verify(transactionRepository).save(savedTransaction);
        verify(transactionEventProducer).publishTransactionDeleted(savedTransaction);
    }

    @Test
    @DisplayName("거래 삭제 실패 - 존재하지 않는 거래")
    void deleteTransaction_NotFound() {
        // Given
        when(transactionRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.deleteTransaction(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND);

        verify(transactionRepository).findByIdAndIsDeletedFalse(999L);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("가계부 거래 요약 조회 성공")
    void getSummaryByLedger_Success() {
        // Given
        Long ledgerId = 1L;
        BigDecimal totalIncome = BigDecimal.valueOf(1000000);
        BigDecimal totalExpense = BigDecimal.valueOf(500000);
        Long transactionCount = 10L;

        when(transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.INCOME))
                .thenReturn(totalIncome);
        when(transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.EXPENSE))
                .thenReturn(totalExpense);
        when(transactionRepository.countByLedgerId(ledgerId)).thenReturn(transactionCount);

        // When
        TransactionSummary result = transactionService.getSummaryByLedger(ledgerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLedgerId()).isEqualTo(ledgerId);
        assertThat(result.getTotalIncome()).isEqualTo(totalIncome);
        assertThat(result.getTotalExpense()).isEqualTo(totalExpense);
        assertThat(result.getBalance()).isEqualTo(BigDecimal.valueOf(500000));
        assertThat(result.getTransactionCount()).isEqualTo(transactionCount);

        verify(transactionRepository).sumAmountByLedgerIdAndType(ledgerId, TransactionType.INCOME);
        verify(transactionRepository).sumAmountByLedgerIdAndType(ledgerId, TransactionType.EXPENSE);
        verify(transactionRepository).countByLedgerId(ledgerId);
    }

    @Test
    @DisplayName("가계부 거래 요약 조회 - 거래 없음")
    void getSummaryByLedger_NoTransactions() {
        // Given
        Long ledgerId = 1L;

        when(transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumAmountByLedgerIdAndType(ledgerId, TransactionType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByLedgerId(ledgerId)).thenReturn(0L);

        // When
        TransactionSummary result = transactionService.getSummaryByLedger(ledgerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpense()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTransactionCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("전체 거래 목록 조회 성공 (페이징 없음)")
    void getAllTransactionsByLedger_Success() {
        // Given
        Long ledgerId = 1L;
        List<Transaction> transactions = List.of(savedTransaction);
        List<TransactionResponse> responses = List.of(transactionResponse);

        when(transactionRepository.findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId))
                .thenReturn(transactions);
        when(transactionMapper.toResponseList(transactions)).thenReturn(responses);

        // When
        List<TransactionResponse> result = transactionService.getAllTransactionsByLedger(ledgerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);

        verify(transactionRepository).findByLedgerIdAndIsDeletedFalseOrderByTransactionDateDescIdDesc(ledgerId);
        verify(transactionMapper).toResponseList(transactions);
    }
}
