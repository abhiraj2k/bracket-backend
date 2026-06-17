package com.expensetracker.budgetservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_goal", schema = "budget_service")
@Getter @Setter @NoArgsConstructor
public class BudgetGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "budget_type", nullable = false, length = 20)
    private String budgetType = "CUSTOM";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
