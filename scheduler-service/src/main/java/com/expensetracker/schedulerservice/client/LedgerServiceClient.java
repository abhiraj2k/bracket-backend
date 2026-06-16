package com.expensetracker.schedulerservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LedgerServiceClient {

    private final RestTemplate restTemplate;

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
}
