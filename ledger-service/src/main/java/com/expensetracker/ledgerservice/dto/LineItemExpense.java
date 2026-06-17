package com.expensetracker.ledgerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @AllArgsConstructor
public class LineItemExpense {
    private UUID categoryId;
    private BigDecimal amount;
    private UUID budgetGoalId;
}
