package com.expensetracker.reportingservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.reportingservice.dto.CreditCardBracketResponse;
import com.expensetracker.reportingservice.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/ledger")
    public Object ledger(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportingService.getLedgerReport(userId, month, year, page, size);
    }

    @GetMapping("/breakdown")
    public Object breakdown(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam int month,
            @RequestParam int year) {
        return reportingService.getCategoryBreakdown(userId, month, year);
    }

    @GetMapping("/credit-card-bracket")
    public ApiResponse<CreditCardBracketResponse> creditCardBracket(
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(reportingService.getCreditCardBracket(userId));
    }
}
