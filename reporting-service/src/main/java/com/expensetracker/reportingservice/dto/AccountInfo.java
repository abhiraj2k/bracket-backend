package com.expensetracker.reportingservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class AccountInfo {
    private String accountType;
    private BigDecimal balance;
    private Boolean isActive;
}
