package com.expensetracker.budgetservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class BudgetGoalCategoryMappingId implements Serializable {

    @Column(name = "budget_goal_id")
    private UUID budgetGoalId;

    @Column(name = "category_id")
    private UUID categoryId;
}
