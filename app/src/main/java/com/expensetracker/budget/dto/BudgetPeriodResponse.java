package com.expensetracker.budget.dto;

import com.expensetracker.budget.entity.BudgetPeriod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter @AllArgsConstructor
public class BudgetPeriodResponse {
    private UUID id;
    private UUID budgetGoalId;
    private String goalName;
    private int periodMonth;
    private int periodYear;
    private BigDecimal startingBalance;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal percentageUsed;
    private String alertLevel;

    public static BudgetPeriodResponse from(BudgetPeriod p) {
        BigDecimal remaining = p.getStartingBalance().subtract(p.getSpentAmount());
        BigDecimal pct = BigDecimal.ZERO;
        if (p.getStartingBalance().compareTo(BigDecimal.ZERO) > 0) {
            pct = p.getSpentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(p.getStartingBalance(), 2, RoundingMode.HALF_UP);
        }
        String alertLevel = computeAlertLevel(p.getBudgetGoal().getBudgetType(), pct);
        return new BudgetPeriodResponse(
                p.getId(), p.getBudgetGoal().getId(), p.getBudgetGoal().getName(),
                p.getPeriodMonth(), p.getPeriodYear(),
                p.getStartingBalance(), p.getSpentAmount(), remaining, pct, alertLevel
        );
    }

    private static String computeAlertLevel(String budgetType, BigDecimal pct) {
        double p = pct.doubleValue();
        return switch (budgetType) {
            case "NEEDS", "WANTS" -> p >= 100 ? "ALERT" : p >= 80 ? "WARNING" : "OK";
            case "INVESTMENTS" -> p >= 100 ? "ALERT" : p >= 50 ? "MILESTONE" : "OK";
            default -> p >= 100 ? "ALERT" : "OK";
        };
    }
}
