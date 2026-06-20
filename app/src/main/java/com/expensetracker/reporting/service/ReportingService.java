package com.expensetracker.reporting.service;

import com.expensetracker.common.dto.PageResponse;
import com.expensetracker.common.enums.AccountType;
import com.expensetracker.common.enums.TransactionType;
import com.expensetracker.ledger.dto.CategoryBreakdownItem;
import com.expensetracker.ledger.dto.TransactionResponse;
import com.expensetracker.ledger.service.TransactionService;
import com.expensetracker.reporting.dto.CreditCardBracketResponse;
import com.expensetracker.user.dto.AccountResponse;
import com.expensetracker.user.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public PageResponse<TransactionResponse> getLedgerReport(String userId, Integer month, Integer year,
                                                             int page, int size) {
        return transactionService.list(UUID.fromString(userId), month, year, PageRequest.of(page, size));
    }

    public List<CategoryBreakdownItem> getCategoryBreakdown(String userId, int month, int year) {
        return transactionService.getBreakdown(UUID.fromString(userId), month, year, TransactionType.EXPENSE);
    }

    public CreditCardBracketResponse getCreditCardBracket(String userId) {
        List<AccountResponse> creditCards = accountService.listActive(UUID.fromString(userId)).stream()
                .filter(a -> a.getAccountType() == AccountType.CREDIT_CARD)
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .toList();

        BigDecimal total = creditCards.stream()
                .map(AccountResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CreditCardBracketResponse(total, creditCards.size());
    }
}
