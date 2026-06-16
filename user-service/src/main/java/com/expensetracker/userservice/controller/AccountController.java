package com.expensetracker.userservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.userservice.dto.AccountResponse;
import com.expensetracker.userservice.dto.CreateAccountRequest;
import com.expensetracker.userservice.dto.UpdateAccountRequest;
import com.expensetracker.userservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.ok("Account created", accountService.create(parseUserId(userId), request));
    }

    @GetMapping
    public ApiResponse<List<AccountResponse>> list(
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(accountService.listActive(parseUserId(userId)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<AccountResponse> update(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateAccountRequest request) {
        return ApiResponse.ok("Account updated", accountService.update(id, parseUserId(userId), request));
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid X-User-Id header: " + userId);
        }
    }
}
