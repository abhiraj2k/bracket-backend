package com.expensetracker.budgetservice.dto;

import com.expensetracker.budgetservice.entity.BudgetGoal;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @AllArgsConstructor
public class BudgetGoalResponse {
    private UUID id;
    private String name;
    private BigDecimal targetAmount;
    private String budgetType;
    private Boolean isActive;

    public static BudgetGoalResponse from(BudgetGoal g) {
        return new BudgetGoalResponse(g.getId(), g.getName(), g.getTargetAmount(),
                g.getBudgetType(), g.getIsActive());
    }
}
