package com.expensetracker.budgetservice.dto;

import com.expensetracker.budgetservice.entity.BudgetGoalCategoryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter @AllArgsConstructor
public class CategoryMappingResponse {
    private UUID budgetGoalId;
    private UUID categoryId;
    private Boolean isDefault;

    public static CategoryMappingResponse from(BudgetGoalCategoryMapping m) {
        return new CategoryMappingResponse(
                m.getId().getBudgetGoalId(),
                m.getId().getCategoryId(),
                m.getIsDefault()
        );
    }
}
