package com.expensetracker.user.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.user.dto.AuthResponse;
import com.expensetracker.user.dto.RegisterRequest;
import com.expensetracker.user.dto.UserResponse;
import com.expensetracker.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("User registered", userService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(userService.getById(UUID.fromString(userId)));
    }
}
