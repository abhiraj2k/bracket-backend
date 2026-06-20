package com.expensetracker.budget.repository;

import com.expensetracker.budget.entity.BudgetGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetGoalRepository extends JpaRepository<BudgetGoal, UUID> {
    List<BudgetGoal> findByUserIdAndIsActiveTrue(UUID userId);
    List<BudgetGoal> findByIsActiveTrue();
    Optional<BudgetGoal> findByIdAndUserId(UUID id, UUID userId);
}
