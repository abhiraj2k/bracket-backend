package com.expensetracker.ledgerservice.client;

import com.expensetracker.ledgerservice.dto.ExpenseEventRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetServiceClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "budgetService", fallbackMethod = "notifyExpenseFallback")
    public void notifyExpense(ExpenseEventRequest event) {
        restTemplate.postForObject(
                "http://budget-service/api/v1/internal/expense-event",
                event,
                Void.class
        );
    }

    private void notifyExpenseFallback(ExpenseEventRequest event, Exception e) {
        log.warn("Budget notification failed (circuit breaker) for user {}: {}",
                event.getUserId(), e.getMessage());
    }
}
