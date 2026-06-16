package com.expensetracker.budgetservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.budgetservice.dto.BudgetGoalResponse;
import com.expensetracker.budgetservice.dto.CreateBudgetGoalRequest;
import com.expensetracker.budgetservice.service.BudgetGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets/goals")
@RequiredArgsConstructor
public class BudgetGoalController {

    private final BudgetGoalService budgetGoalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BudgetGoalResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateBudgetGoalRequest request) {
        return ApiResponse.ok("Budget goal created", budgetGoalService.create(parseUserId(userId), request));
    }

    @GetMapping
    public ApiResponse<List<BudgetGoalResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(budgetGoalService.listActive(parseUserId(userId)));
    }

    private UUID parseUserId(String userId) {
        try { return UUID.fromString(userId); }
        catch (IllegalArgumentException e) { throw new ValidationException("Invalid X-User-Id: " + userId); }
    }
}
