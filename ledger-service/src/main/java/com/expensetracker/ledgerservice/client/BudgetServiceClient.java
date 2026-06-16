package com.expensetracker.ledgerservice.client;

import com.expensetracker.ledgerservice.dto.ExpenseEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceClient {

    private final RestTemplate restTemplate;

    public void notifyExpense(ExpenseEventRequest event) {
        try {
            restTemplate.postForObject(
                    "http://budget-service/api/v1/internal/expense-event",
                    event,
                    Void.class
            );
        } catch (Exception e) {
            // best-effort: transaction already committed; log and continue
            log.warn("Budget notification failed for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
