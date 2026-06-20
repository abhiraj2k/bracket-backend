package com.expensetracker.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private UUID userId;
    private UUID householdId;
    private String name;
    private String email;
}
