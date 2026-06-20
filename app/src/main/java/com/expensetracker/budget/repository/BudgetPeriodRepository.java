package com.expensetracker.budget.repository;

import com.expensetracker.budget.entity.BudgetPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetPeriodRepository extends JpaRepository<BudgetPeriod, UUID> {
    List<BudgetPeriod> findByBudgetGoal_UserIdAndPeriodMonthAndPeriodYear(UUID userId, int month, int year);
    Optional<BudgetPeriod> findByBudgetGoalIdAndPeriodMonthAndPeriodYear(UUID goalId, int month, int year);
    List<BudgetPeriod> findByBudgetGoal_IsActiveTrueAndPeriodMonthAndPeriodYear(int month, int year);
}
