package com.expensetracker.user.dto;

import com.expensetracker.common.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class BalanceAdjustRequest {
    private UUID sourceAccountId;
    private UUID destAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
}
