package com.expensetracker.metadata.dto;

import com.expensetracker.common.enums.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class CreateCategoryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private CategoryType categoryType;

    private UUID parentCategoryId;
}
