package com.hamkkebu.transactionservice.data.dto;

import com.hamkkebu.transactionservice.data.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private Long ledgerId;
    private Long userId;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private String category;
    private LocalDate transactionDate;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
