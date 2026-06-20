package com.expensetracker.metadata.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateCategoryAliasRequest {

    @Size(max = 100)
    private String customAlias;
}
