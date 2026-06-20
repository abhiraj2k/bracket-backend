package com.expensetracker.ledger.dto;

import com.expensetracker.common.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @AllArgsConstructor
public class BalanceAdjustRequest {
    private UUID sourceAccountId;
    private UUID destAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
}
