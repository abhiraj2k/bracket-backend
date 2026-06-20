package com.expensetracker.scheduler.dto;

import com.expensetracker.common.enums.RecurrenceFrequency;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class CreateRecurringTransactionRequest {

    @NotNull
    private RecurrenceFrequency frequency;

    @NotNull
    private LocalDate nextExecutionDate;

    @NotNull
    private Map<String, Object> headerTemplate;

    @NotEmpty
    private List<Map<String, Object>> lineItemsTemplate;
}
