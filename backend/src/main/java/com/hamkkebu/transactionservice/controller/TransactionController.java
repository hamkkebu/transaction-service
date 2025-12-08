package com.hamkkebu.transactionservice.controller;

import com.hamkkebu.boilerplate.common.dto.ApiResponse;
import com.hamkkebu.transactionservice.data.dto.TransactionRequest;
import com.hamkkebu.transactionservice.data.dto.TransactionResponse;
import com.hamkkebu.transactionservice.data.dto.TransactionSummary;
import com.hamkkebu.transactionservice.security.CurrentUser;
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
import org.springframework.web.bind.annotation.*;

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
            @PathVariable Long id) {

        log.info("GET /api/v1/transactions/{}", id);
        TransactionResponse transaction = transactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping
    @Operation(summary = "거래 목록 조회 (페이징)", description = "특정 가계부의 거래 목록을 페이징으로 조회합니다")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam Long ledgerId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/transactions?ledgerId={}", ledgerId);
        Page<TransactionResponse> transactions = transactionService.getTransactionsByLedger(ledgerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/all")
    @Operation(summary = "거래 전체 목록 조회", description = "특정 가계부의 모든 거래를 조회합니다")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @RequestParam Long ledgerId) {

        log.info("GET /api/v1/transactions/all?ledgerId={}", ledgerId);
        List<TransactionResponse> transactions = transactionService.getAllTransactionsByLedger(ledgerId);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/summary")
    @Operation(summary = "거래 요약 조회", description = "특정 가계부의 거래 요약 정보를 조회합니다")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @RequestParam Long ledgerId) {

        log.info("GET /api/v1/transactions/summary?ledgerId={}", ledgerId);
        TransactionSummary summary = transactionService.getSummaryByLedger(ledgerId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PutMapping("/{id}")
    @Operation(summary = "거래 수정", description = "거래 정보를 수정합니다")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {

        log.info("PUT /api/v1/transactions/{}", id);
        TransactionResponse transaction = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "거래 삭제", description = "거래를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long id) {

        log.info("DELETE /api/v1/transactions/{}", id);
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
