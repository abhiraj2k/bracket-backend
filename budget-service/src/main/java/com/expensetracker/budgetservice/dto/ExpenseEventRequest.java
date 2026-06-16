package com.expensetracker.budgetservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
public class ExpenseEventRequest {
    private UUID userId;
    private Instant transactionDate;
    private BigDecimal totalAmount;
}
