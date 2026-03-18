package com.hamkkebu.transactionservice.codef.controller;

import com.hamkkebu.boilerplate.common.dto.ApiResponse;
import com.hamkkebu.boilerplate.common.user.annotation.CurrentUser;
import com.hamkkebu.transactionservice.codef.dto.*;
import com.hamkkebu.transactionservice.codef.service.CodefCardService;
import com.hamkkebu.transactionservice.codef.service.CodefSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Codef 카드 연동 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Integration", description = "Codef 카드 연동 API")
public class LinkedCardController {

    private final CodefCardService codefCardService;
    private final CodefSyncService codefSyncService;

    /**
     * 카드사 계정 등록 (Connected ID 발급)
     */
    @PostMapping("/connect")
    @Operation(summary = "카드사 계정 연결", description = "카드사 로그인 정보로 Codef Connected ID를 발급받습니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectAccount(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody CodefAccountRequest request) {

        log.info("POST /api/v1/cards/connect - userId={}, org={}", userId, request.getOrganization());
        Map<String, Object> result = codefCardService.connectCardAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 간편인증 2차 확인 요청
     */
    @PostMapping("/connect/simple-auth")
    @Operation(summary = "간편인증 확인",
            description = "사용자가 앱에서 인증을 완료한 후 2차 확인 요청을 보냅니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmSimpleAuth(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody SimpleAuthRequest request) {

        log.info("POST /api/v1/cards/connect/simple-auth - userId={}, org={}",
                userId, request.getOrganization());
        Map<String, Object> result = codefCardService.confirmSimpleAuth(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 기존 Connected ID에 추가 카드사 계정 등록
     */
    @PostMapping("/connect/{connectedId}")
    @Operation(summary = "추가 카드사 계정 연결", description = "기존 Connected ID에 새로운 카드사 계정을 추가합니다")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addAccount(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable String connectedId,
            @Valid @RequestBody CodefAccountRequest request) {

        log.info("POST /api/v1/cards/connect/{} - userId={}, org={}",
                connectedId, userId, request.getOrganization());
        Map<String, Object> result = codefCardService.addCardAccount(userId, connectedId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 보유카드 목록 조회 (연동할 카드 선택용)
     */
    @GetMapping("/list")
    @Operation(summary = "보유카드 조회", description = "Codef를 통해 카드사에 등록된 보유카드 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<CodefCardInfo>>> getCardList(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @RequestParam String connectedId,
            @RequestParam String organization) {

        log.info("GET /api/v1/cards/list - userId={}, org={}", userId, organization);
        List<CodefCardInfo> cards = codefCardService.getCardList(userId, connectedId, organization);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    /**
     * 선택한 카드를 가계부에 연동
     */
    @PostMapping("/link")
    @Operation(summary = "카드 연동", description = "선택한 카드를 가계부에 연동합니다")
    public ResponseEntity<ApiResponse<LinkedCardResponse>> linkCard(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @Valid @RequestBody LinkCardRequest request) {

        log.info("POST /api/v1/cards/link - userId={}, ledgerId={}", userId, request.getLedgerId());
        LinkedCardResponse response = codefCardService.linkCard(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 내 연동 카드 목록 조회
     */
    @GetMapping
    @Operation(summary = "연동 카드 목록", description = "내 연동 카드 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<LinkedCardResponse>>> getLinkedCards(
            @Parameter(hidden = true) @CurrentUser Long userId) {

        log.info("GET /api/v1/cards - userId={}", userId);
        List<LinkedCardResponse> cards = codefCardService.getLinkedCards(userId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    /**
     * 특정 가계부의 연동 카드 목록 조회
     */
    @GetMapping("/ledger/{ledgerId}")
    @Operation(summary = "가계부별 연동 카드 목록", description = "특정 가계부의 연동 카드 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<LinkedCardResponse>>> getLinkedCardsByLedger(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long ledgerId) {

        log.info("GET /api/v1/cards/ledger/{} - userId={}", ledgerId, userId);
        List<LinkedCardResponse> cards = codefCardService.getLinkedCardsByLedger(userId, ledgerId);
        return ResponseEntity.ok(ApiResponse.success(cards));
    }

    /**
     * 카드 연동 해제
     */
    @DeleteMapping("/{linkedCardId}")
    @Operation(summary = "카드 연동 해제",
            description = "카드 연동을 해제합니다. 해당 카드의 거래 내역도 함께 삭제됩니다. 30일 내 복구 가능합니다.")
    public ResponseEntity<ApiResponse<Void>> unlinkCard(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long linkedCardId) {

        log.info("DELETE /api/v1/cards/{} - userId={}", linkedCardId, userId);
        codefCardService.unlinkCard(userId, linkedCardId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 카드 승인내역 수동 동기화
     */
    @PostMapping("/{linkedCardId}/sync")
    @Operation(summary = "승인내역 동기화",
            description = "연동된 카드의 승인내역을 Codef에서 가져와 거래 내역으로 등록합니다")
    public ResponseEntity<ApiResponse<SyncResult>> syncCard(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long linkedCardId) {

        log.info("POST /api/v1/cards/{}/sync - userId={}", linkedCardId, userId);
        SyncResult result = codefSyncService.syncCard(userId, linkedCardId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
