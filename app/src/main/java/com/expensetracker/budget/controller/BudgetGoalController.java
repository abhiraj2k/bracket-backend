package com.expensetracker.budget.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.budget.dto.BudgetGoalResponse;
import com.expensetracker.budget.dto.CreateBudgetGoalRequest;
import com.expensetracker.budget.service.BudgetGoalService;
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-User-Id") String userId, @PathVariable UUID id) {
        budgetGoalService.delete(parseUserId(userId), id);
    }

    private UUID parseUserId(String userId) {
        try { return UUID.fromString(userId); }
        catch (IllegalArgumentException e) { throw new ValidationException("Invalid X-User-Id: " + userId); }
    }
}
