package com.hamkkebu.transactionservice.controller;

import com.hamkkebu.boilerplate.common.dto.ApiResponse;
import com.hamkkebu.boilerplate.common.dto.PageResponseDto;
import com.hamkkebu.transactionservice.data.dto.PeriodTransactionSummary;
import com.hamkkebu.transactionservice.data.dto.TransactionRequest;
import com.hamkkebu.transactionservice.data.dto.TransactionResponse;
import com.hamkkebu.transactionservice.data.dto.TransactionSummary;
import com.hamkkebu.boilerplate.common.user.annotation.CurrentUser;
import com.hamkkebu.transactionservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction", description = "거래 관리 API")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "거래 생성", description = "새로운 거래를 생성합니다")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody TransactionRequest request) {

        log.info("POST /api/v1/transactions - userId: {}, ledgerId: {}", userId, request.getLedgerId());
        TransactionResponse transaction = transactionService.createTransaction(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(transaction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "거래 상세 조회", description = "특정 거래의 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long id) {

        log.info("GET /api/v1/transactions/{} - userId: {}", id, userId);
        TransactionResponse transaction = transactionService.getTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping
    @Operation(summary = "거래 목록 조회 (페이징)", description = "특정 가계부의 거래 목록을 페이징으로 조회합니다")
    public ResponseEntity<ApiResponse<PageResponseDto<TransactionResponse>>> getTransactions(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/transactions?ledgerId={} - userId: {}", ledgerId, userId);
        Page<TransactionResponse> transactions = transactionService.getTransactionsByLedger(ledgerId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponseDto.of(transactions)));
    }

    @GetMapping("/all")
    @Operation(summary = "거래 전체 목록 조회", description = "특정 가계부의 모든 거래를 조회합니다")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId) {

        log.info("GET /api/v1/transactions/all?ledgerId={} - userId: {}", ledgerId, userId);
        List<TransactionResponse> transactions = transactionService.getAllTransactionsByLedger(ledgerId, userId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/summary")
    @Operation(summary = "거래 요약 조회", description = "특정 가계부의 거래 요약 정보를 조회합니다")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId) {

        log.info("GET /api/v1/transactions/summary?ledgerId={} - userId: {}", ledgerId, userId);
        TransactionSummary summary = transactionService.getSummaryByLedger(ledgerId, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PutMapping("/{id}")
    @Operation(summary = "거래 수정", description = "거래 정보를 수정합니다")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {

        log.info("PUT /api/v1/transactions/{} - userId: {}", id, userId);
        TransactionResponse transaction = transactionService.updateTransaction(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "거래 삭제", description = "거래를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long id) {

        log.info("DELETE /api/v1/transactions/{} - userId: {}", id, userId);
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== 기간별 조회 API ====================

    @GetMapping("/daily")
    @Operation(summary = "일별 거래 요약 조회", description = "특정 날짜의 거래 요약을 조회합니다")
    public ResponseEntity<ApiResponse<PeriodTransactionSummary>> getDailySummary(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("GET /api/v1/transactions/daily?ledgerId={}&date={} - userId: {}", ledgerId, date, userId);
        PeriodTransactionSummary summary = transactionService.getDailySummary(ledgerId, date, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/monthly")
    @Operation(summary = "월별 거래 요약 조회", description = "특정 월의 거래 요약을 조회합니다 (일별 상세 포함)")
    public ResponseEntity<ApiResponse<PeriodTransactionSummary>> getMonthlySummary(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam int month) {

        log.info("GET /api/v1/transactions/monthly?ledgerId={}&year={}&month={} - userId: {}", ledgerId, year, month, userId);
        PeriodTransactionSummary summary = transactionService.getMonthlySummary(ledgerId, year, month, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/yearly")
    @Operation(summary = "년별 거래 요약 조회", description = "특정 연도의 거래 요약을 조회합니다 (월별 상세 포함)")
    public ResponseEntity<ApiResponse<PeriodTransactionSummary>> getYearlySummary(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId,
            @RequestParam int year) {

        log.info("GET /api/v1/transactions/yearly?ledgerId={}&year={} - userId: {}", ledgerId, year, userId);
        PeriodTransactionSummary summary = transactionService.getYearlySummary(ledgerId, year, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/period")
    @Operation(summary = "기간별 거래 요약 조회", description = "지정한 기간의 거래 요약을 조회합니다")
    public ResponseEntity<ApiResponse<PeriodTransactionSummary>> getPeriodSummary(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam Long ledgerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/v1/transactions/period?ledgerId={}&startDate={}&endDate={} - userId: {}", ledgerId, startDate, endDate, userId);
        PeriodTransactionSummary summary = transactionService.getPeriodSummary(ledgerId, startDate, endDate, userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
