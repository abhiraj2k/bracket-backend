package com.expensetracker.scheduler.dto;

import com.expensetracker.common.enums.RecurrenceFrequency;
import com.expensetracker.scheduler.entity.RecurringTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter @AllArgsConstructor
public class RecurringTransactionResponse {
    private UUID id;
    private RecurrenceFrequency frequency;
    private LocalDate nextExecutionDate;
    private Map<String, Object> headerTemplate;
    private List<Map<String, Object>> lineItemsTemplate;
    private Boolean isActive;

    public static RecurringTransactionResponse from(RecurringTransaction r) {
        return new RecurringTransactionResponse(
                r.getId(), r.getFrequency(), r.getNextExecutionDate(),
                r.getHeaderTemplate(), r.getLineItemsTemplate(), r.getIsActive()
        );
    }
}
