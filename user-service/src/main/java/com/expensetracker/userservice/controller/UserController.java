package com.expensetracker.userservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.userservice.dto.RegisterRequest;
import com.expensetracker.userservice.dto.UserResponse;
import com.expensetracker.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("User registered", userService.register(request));
    }
}
