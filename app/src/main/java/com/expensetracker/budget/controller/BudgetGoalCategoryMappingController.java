package com.expensetracker.budget.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.budget.dto.AddCategoryMappingRequest;
import com.expensetracker.budget.dto.CategoryMappingResponse;
import com.expensetracker.budget.service.BudgetGoalCategoryMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets/goals/{goalId}/categories")
@RequiredArgsConstructor
public class BudgetGoalCategoryMappingController {

    private final BudgetGoalCategoryMappingService mappingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryMappingResponse> add(
            @PathVariable UUID goalId,
            @Valid @RequestBody AddCategoryMappingRequest request) {
        return ApiResponse.ok("Mapping added", mappingService.addMapping(goalId, request));
    }

    @GetMapping
    public ApiResponse<List<CategoryMappingResponse>> list(@PathVariable UUID goalId) {
        return ApiResponse.ok(mappingService.listMappings(goalId));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID goalId, @PathVariable UUID categoryId) {
        mappingService.deleteMapping(goalId, categoryId);
    }
}
