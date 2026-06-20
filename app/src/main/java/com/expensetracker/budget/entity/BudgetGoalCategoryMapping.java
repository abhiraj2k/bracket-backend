package com.expensetracker.budget.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "budget_goal_category_mapping", schema = "budget_service")
@Getter @Setter @NoArgsConstructor
public class BudgetGoalCategoryMapping {

    @EmbeddedId
    private BudgetGoalCategoryMappingId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("budgetGoalId")
    @JoinColumn(name = "budget_goal_id")
    private BudgetGoal budgetGoal;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    public BudgetGoalCategoryMapping(BudgetGoal goal, java.util.UUID categoryId, Boolean isDefault) {
        this.id = new BudgetGoalCategoryMappingId(goal.getId(), categoryId);
        this.budgetGoal = goal;
        this.isDefault = isDefault;
    }
}
