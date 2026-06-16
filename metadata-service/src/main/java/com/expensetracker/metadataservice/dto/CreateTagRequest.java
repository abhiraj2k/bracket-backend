package com.expensetracker.metadataservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateTagRequest {

    @NotBlank @Size(max = 50)
    private String name;
}
