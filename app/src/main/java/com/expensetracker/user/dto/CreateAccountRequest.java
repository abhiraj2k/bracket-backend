package com.expensetracker.user.dto;

import com.expensetracker.common.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CreateAccountRequest {

    @NotBlank
    private String name;

    @NotNull
    private AccountType accountType;

    @NotNull @DecimalMin("0.0")
    private BigDecimal openingBalance;

    private String currencyCode = "INR";

    private Integer billingStartDay;
    private Integer billingEndDay;
}
