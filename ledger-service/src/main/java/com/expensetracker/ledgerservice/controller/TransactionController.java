package com.expensetracker.ledgerservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.dto.PageResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.ledgerservice.dto.CreateTransactionRequest;
import com.expensetracker.ledgerservice.dto.TransactionResponse;
import com.expensetracker.ledgerservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateTransactionRequest request) {
        return ApiResponse.ok("Transaction created", transactionService.create(parseUserId(userId), request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> get(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(transactionService.get(id, parseUserId(userId)));
    }

    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> list(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.ok(transactionService.list(parseUserId(userId), month, year, pageable));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        transactionService.delete(id, parseUserId(userId));
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid X-User-Id header: " + userId);
        }
    }
}
