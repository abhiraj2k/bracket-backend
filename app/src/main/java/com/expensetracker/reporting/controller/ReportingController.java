package com.expensetracker.reporting.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.dto.PageResponse;
import com.expensetracker.ledger.dto.CategoryBreakdownItem;
import com.expensetracker.ledger.dto.TransactionResponse;
import com.expensetracker.reporting.dto.CreditCardBracketResponse;
import com.expensetracker.reporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/ledger")
    public ApiResponse<PageResponse<TransactionResponse>> ledger(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if ((month == null) != (year == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month and year must both be provided or both omitted");
        }
        return ApiResponse.ok(reportingService.getLedgerReport(userId, month, year, page, size));
    }

    @GetMapping("/breakdown")
    public ApiResponse<List<CategoryBreakdownItem>> breakdown(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam int month,
            @RequestParam int year) {
        return ApiResponse.ok(reportingService.getCategoryBreakdown(userId, month, year));
    }

    @GetMapping("/credit-card-bracket")
    public ApiResponse<CreditCardBracketResponse> creditCardBracket(
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(reportingService.getCreditCardBracket(userId));
    }
}
