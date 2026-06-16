package com.expensetracker.metadataservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.metadataservice.dto.CategoryResponse;
import com.expensetracker.metadataservice.dto.UpdateCategoryAliasRequest;
import com.expensetracker.metadataservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(categoryService.getUserCategories(parseUserId(userId)));
    }

    @PutMapping("/mapping/{categoryId}")
    public ApiResponse<CategoryResponse> updateAlias(
            @PathVariable UUID categoryId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateCategoryAliasRequest request) {
        return ApiResponse.ok("Alias updated",
                categoryService.updateAlias(parseUserId(userId), categoryId, request));
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid X-User-Id header: " + userId);
        }
    }
}
