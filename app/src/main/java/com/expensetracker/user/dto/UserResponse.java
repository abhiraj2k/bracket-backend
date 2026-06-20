package com.expensetracker.user.dto;

import com.expensetracker.user.entity.AppUser;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter @AllArgsConstructor
public class UserResponse {
    private UUID userId;
    private UUID householdId;
    private String name;
    private String email;
    private String baseCurrency;

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getHouseholdId(),
                user.getName(),
                user.getEmail(),
                user.getBaseCurrency()
        );
    }
}
