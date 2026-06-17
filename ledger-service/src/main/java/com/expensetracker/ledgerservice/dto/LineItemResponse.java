package com.expensetracker.ledgerservice.dto;

import com.expensetracker.ledgerservice.entity.TransactionLineItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Getter @AllArgsConstructor
public class LineItemResponse {
    private UUID id;
    private UUID categoryId;
    private UUID budgetGoalId;
    private BigDecimal amount;
    private Set<UUID> tagIds;

    public static LineItemResponse from(TransactionLineItem item) {
        return new LineItemResponse(item.getId(), item.getCategoryId(), item.getBudgetGoalId(),
                item.getAmount(), item.getTagIds());
    }
}
