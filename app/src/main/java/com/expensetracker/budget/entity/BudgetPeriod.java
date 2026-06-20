package com.expensetracker.budget.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_period", schema = "budget_service")
@Getter @Setter @NoArgsConstructor
public class BudgetPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "budget_goal_id", nullable = false)
    private BudgetGoal budgetGoal;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "starting_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal startingBalance;

    @Column(name = "spent_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;
}
