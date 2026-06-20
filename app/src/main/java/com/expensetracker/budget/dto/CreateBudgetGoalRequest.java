package com.expensetracker.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateBudgetGoalRequest {

    @NotBlank @Size(max = 50)
    private String name;

    @NotNull @DecimalMin("0.01")
    private BigDecimal targetAmount;

    private String budgetType = "CUSTOM";
}
