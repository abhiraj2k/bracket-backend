package com.expensetracker.ledgerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @AllArgsConstructor
public class ExpenseEventRequest {
    private UUID userId;
    private Instant transactionDate;
    private List<LineItemExpense> lineItems;
}
