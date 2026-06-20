package com.expensetracker.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class CreditCardBracketResponse {
    private BigDecimal totalCreditCardLiability;
    private int accountCount;
}
