package com.expensetracker.userservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.userservice.dto.AuthResponse;
import com.expensetracker.userservice.dto.LoginRequest;
import com.expensetracker.userservice.service.UserService;
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
