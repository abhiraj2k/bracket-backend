package com.expensetracker.budgetservice.controller;

import com.expensetracker.budgetservice.dto.ExpenseEventRequest;
import com.expensetracker.budgetservice.service.BudgetPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final BudgetPeriodService budgetPeriodService;

    @PostMapping("/expense-event")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleExpenseEvent(@RequestBody ExpenseEventRequest event) {
        budgetPeriodService.handleExpenseEvent(event);
    }
}
