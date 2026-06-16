package com.expensetracker.reportingservice.service;

import com.expensetracker.reportingservice.dto.AccountInfo;
import com.expensetracker.reportingservice.dto.CreditCardBracketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final RestTemplate restTemplate;

    public Object getLedgerReport(String userId, Integer month, Integer year, int page, int size) {
        String url = "http://ledger-service/api/v1/transactions?page={page}&size={size}"
                + (month != null ? "&month={month}&year={year}" : "");

        HttpEntity<?> entity = userHeader(userId);

        if (month != null) {
            return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class,
                    page, size, month, year).getBody();
        }
        return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class, page, size).getBody();
    }

    public Object getCategoryBreakdown(String userId, int month, int year) {
        String url = "http://ledger-service/api/v1/internal/breakdown"
                + "?userId={userId}&month={month}&year={year}";
        return restTemplate.exchange(url, HttpMethod.GET, userHeader(userId), Object.class,
                UUID.fromString(userId), month, year).getBody();
    }

    public CreditCardBracketResponse getCreditCardBracket(String userId) {
        String url = "http://user-service/api/v1/accounts";

        List<AccountInfo> accounts = fetchAccounts(userId, url);

        List<AccountInfo> creditCards = accounts.stream()
                .filter(a -> "CREDIT_CARD".equals(a.getAccountType()))
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .toList();

        BigDecimal total = creditCards.stream()
                .map(AccountInfo::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CreditCardBracketResponse(total, creditCards.size());
    }

    @SuppressWarnings("unchecked")
    private List<AccountInfo> fetchAccounts(String userId, String url) {
        var response = restTemplate.exchange(url, HttpMethod.GET, userHeader(userId),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> body = Objects.requireNonNull(response.getBody());
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) body.get("data");
        if (rawList == null) return List.of();

        return rawList.stream().map(m -> {
            AccountInfo info = new AccountInfo();
            info.setAccountType((String) m.get("accountType"));
            info.setIsActive((Boolean) m.get("isActive"));
            Object bal = m.get("balance");
            info.setBalance(bal == null ? BigDecimal.ZERO : new BigDecimal(bal.toString()));
            return info;
        }).toList();
    }

    private HttpEntity<?> userHeader(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        return new HttpEntity<>(headers);
    }
}
