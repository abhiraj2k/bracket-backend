package com.expensetracker.budget.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class AddCategoryMappingRequest {

    @NotNull
    private UUID categoryId;

    private boolean isDefault = false;
}
