package com.expensetracker.reporting.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class AccountInfo {
    private String accountType;
    private BigDecimal balance;
    private Boolean isActive;
}
