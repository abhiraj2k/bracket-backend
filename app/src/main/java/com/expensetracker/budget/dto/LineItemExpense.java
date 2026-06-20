package com.expensetracker.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LineItemExpense {
    private UUID categoryId;
    private BigDecimal amount;
    private UUID budgetGoalId;
}
