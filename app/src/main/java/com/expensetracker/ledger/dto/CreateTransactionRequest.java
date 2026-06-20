package com.expensetracker.ledger.dto;

import com.expensetracker.common.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class CreateTransactionRequest {

    @NotNull
    private UUID sourceAccountId;

    private UUID destAccountId;

    @NotNull
    private TransactionType transactionType;

    @NotNull @DecimalMin(value = "0.01", message = "total_amount must be > 0")
    private BigDecimal totalAmount;

    @NotNull
    private Instant transactionDate;

    private String note;

    @NotEmpty
    @Valid
    private List<LineItemRequest> lineItems;
}
