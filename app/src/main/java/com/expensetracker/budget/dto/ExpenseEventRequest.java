package com.expensetracker.budget.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class ExpenseEventRequest {
    private UUID userId;
    private Instant transactionDate;
    private List<LineItemExpense> lineItems;
}
