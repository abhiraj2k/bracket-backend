package com.expensetracker.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {

    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    // password accepted but auth/hashing deferred to security phase
    @NotBlank
    private String password;
}
