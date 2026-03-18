package com.hamkkebu.transactionservice.controller;

import com.hamkkebu.boilerplate.common.dto.ApiResponse;
import com.hamkkebu.boilerplate.common.user.annotation.CurrentUser;
import com.hamkkebu.transactionservice.data.entity.Ledger;
import com.hamkkebu.transactionservice.repository.LedgerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 가계부 조회 API (Ledger Service에서 Kafka로 동기화된 데이터)
 *
 * <p>Transaction Service에서 사용하는 가계부 목록을 제공합니다.</p>
 * <p>데이터는 ledger-service에서 Kafka 이벤트로 동기화됩니다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ledgers")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "가계부 조회 API (동기화된 데이터)")
public class LedgerController {

    private final LedgerRepository ledgerRepository;

    @GetMapping
    @Operation(summary = "내 가계부 목록 조회", description = "현재 사용자의 가계부 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyLedgers(
            @Parameter(hidden = true) @CurrentUser Long currentUserId
    ) {
        log.debug("가계부 목록 조회 - userId: {}", currentUserId);

        List<Ledger> ledgers = ledgerRepository.findByUserIdAndIsDeletedFalse(currentUserId);

        List<Map<String, Object>> response = ledgers.stream()
                .map(ledger -> Map.<String, Object>of(
                        "ledgerId", ledger.getLedgerId(),
                        "name", ledger.getName(),
                        "description", ledger.getDescription() != null ? ledger.getDescription() : "",
                        "currency", ledger.getCurrency(),
                        "isDefault", ledger.getIsDefault()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
