package com.expensetracker.ledgerservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.enums.TransactionType;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.ledgerservice.dto.CategoryBreakdownItem;
import com.expensetracker.ledgerservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final TransactionService transactionService;

    @GetMapping("/breakdown")
    public ApiResponse<List<CategoryBreakdownItem>> breakdown(
            @RequestParam UUID userId,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "EXPENSE") TransactionType type) {
        return ApiResponse.ok(transactionService.getBreakdown(userId, month, year, type));
    }
}
