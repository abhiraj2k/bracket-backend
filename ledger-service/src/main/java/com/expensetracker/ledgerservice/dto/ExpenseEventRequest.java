package com.expensetracker.ledgerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @AllArgsConstructor
public class ExpenseEventRequest {
    private UUID userId;
    private Instant transactionDate;
    private BigDecimal totalAmount;
}
