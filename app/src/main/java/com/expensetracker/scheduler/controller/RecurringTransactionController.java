package com.expensetracker.scheduler.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.scheduler.dto.CreateRecurringTransactionRequest;
import com.expensetracker.scheduler.dto.RecurringTransactionResponse;
import com.expensetracker.scheduler.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecurringTransactionResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateRecurringTransactionRequest request) {
        return ApiResponse.ok("Recurring transaction created", service.create(parseUserId(userId), request));
    }

    @GetMapping
    public ApiResponse<List<RecurringTransactionResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(service.listActive(parseUserId(userId)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @RequestHeader("X-User-Id") String userId) {
        service.deactivate(id, parseUserId(userId));
    }

    private UUID parseUserId(String userId) {
        try { return UUID.fromString(userId); }
        catch (IllegalArgumentException e) { throw new ValidationException("Invalid X-User-Id: " + userId); }
    }
}
