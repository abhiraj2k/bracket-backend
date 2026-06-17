package com.expensetracker.schedulerservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceClient {

    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "ledgerService", fallbackMethod = "createTransactionFallback")
    public void createTransaction(UUID userId, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId.toString());

        restTemplate.postForObject(
                "http://ledger-service/api/v1/transactions",
                new HttpEntity<>(payload, headers),
                Void.class
        );
    }

    private void createTransactionFallback(UUID userId, Map<String, Object> payload, Exception e) {
        throw new RuntimeException("Ledger service unavailable: " + e.getMessage(), e);
    }
}
