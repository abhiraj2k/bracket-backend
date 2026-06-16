package com.expensetracker.budgetservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.budgetservice.dto.BudgetPeriodResponse;
import com.expensetracker.budgetservice.service.BudgetPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets/periods")
@RequiredArgsConstructor
public class BudgetPeriodController {

    private final BudgetPeriodService budgetPeriodService;

    @GetMapping("/current")
    public ApiResponse<List<BudgetPeriodResponse>> current(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(budgetPeriodService.getCurrentPeriods(parseUserId(userId)));
    }

    private UUID parseUserId(String userId) {
        try { return UUID.fromString(userId); }
        catch (IllegalArgumentException e) { throw new ValidationException("Invalid X-User-Id: " + userId); }
    }
}
