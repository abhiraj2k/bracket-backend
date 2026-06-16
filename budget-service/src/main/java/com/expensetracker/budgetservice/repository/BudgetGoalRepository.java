package com.expensetracker.budgetservice.repository;

import com.expensetracker.budgetservice.entity.BudgetGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetGoalRepository extends JpaRepository<BudgetGoal, UUID> {
    List<BudgetGoal> findByUserIdAndIsActiveTrue(UUID userId);
    List<BudgetGoal> findByIsActiveTrue();
}
