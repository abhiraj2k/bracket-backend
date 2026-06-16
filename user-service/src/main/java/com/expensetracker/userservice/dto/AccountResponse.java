package com.expensetracker.userservice.dto;

import com.expensetracker.common.enums.AccountType;
import com.expensetracker.userservice.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @AllArgsConstructor
public class AccountResponse {
    private UUID id;
    private String name;
    private AccountType accountType;
    private BigDecimal balance;
    private String currencyCode;
    private Integer billingStartDay;
    private Integer billingEndDay;
    private Boolean isActive;

    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(), a.getName(), a.getAccountType(),
                a.getBalance(), a.getCurrencyCode(),
                a.getBillingStartDay(), a.getBillingEndDay(), a.getIsActive()
        );
    }
}
