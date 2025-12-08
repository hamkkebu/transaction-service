package com.hamkkebu.transactionservice.data.dto;

import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "가계부 ID는 필수입니다")
    private Long ledgerId;

    @NotNull(message = "거래 유형은 필수입니다")
    private TransactionType type;

    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다")
    private BigDecimal amount;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    private String description;

    @Size(max = 100, message = "카테고리는 100자를 초과할 수 없습니다")
    private String category;

    @NotNull(message = "거래 날짜는 필수입니다")
    private LocalDate transactionDate;

    @Size(max = 1000, message = "메모는 1000자를 초과할 수 없습니다")
    private String memo;
}
