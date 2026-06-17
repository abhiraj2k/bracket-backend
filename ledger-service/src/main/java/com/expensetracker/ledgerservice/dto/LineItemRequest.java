package com.expensetracker.ledgerservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter @Setter
public class LineItemRequest {

    private UUID categoryId;

    private UUID budgetGoalId;

    @NotNull @DecimalMin(value = "0.01", message = "line item amount must be > 0")
    private BigDecimal amount;

    private Set<UUID> tagIds = new HashSet<>();
}
