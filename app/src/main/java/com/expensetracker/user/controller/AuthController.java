package com.expensetracker.user.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.user.dto.AuthResponse;
import com.expensetracker.user.dto.LoginRequest;
import com.expensetracker.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", userService.login(request));
    }
}
