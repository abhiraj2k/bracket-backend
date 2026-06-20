package com.expensetracker.budget.repository;

import com.expensetracker.budget.entity.BudgetGoalCategoryMapping;
import com.expensetracker.budget.entity.BudgetGoalCategoryMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetGoalCategoryMappingRepository
        extends JpaRepository<BudgetGoalCategoryMapping, BudgetGoalCategoryMappingId> {

    List<BudgetGoalCategoryMapping> findByBudgetGoalId(UUID budgetGoalId);

    Optional<BudgetGoalCategoryMapping> findFirstByIdCategoryIdAndIsDefaultTrue(UUID categoryId);
}
