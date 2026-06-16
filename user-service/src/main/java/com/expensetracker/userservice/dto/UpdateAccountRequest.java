package com.expensetracker.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateAccountRequest {
    private String name;
    private Boolean isActive;
}
