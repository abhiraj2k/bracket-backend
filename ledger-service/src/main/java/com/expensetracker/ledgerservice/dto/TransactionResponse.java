package com.expensetracker.ledgerservice.dto;

import com.expensetracker.common.enums.TransactionType;
import com.expensetracker.ledgerservice.entity.TransactionHeader;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @AllArgsConstructor
public class TransactionResponse {
    private UUID id;
    private UUID userId;
    private UUID sourceAccountId;
    private UUID destAccountId;
    private TransactionType transactionType;
    private BigDecimal totalAmount;
    private Instant transactionDate;
    private String note;
    private Instant createdAt;
    private List<LineItemResponse> lineItems;

    public static TransactionResponse from(TransactionHeader h) {
        List<LineItemResponse> items = h.getLineItems().stream()
                .map(LineItemResponse::from).toList();
        return new TransactionResponse(
                h.getId(), h.getUserId(), h.getSourceAccountId(), h.getDestAccountId(),
                h.getTransactionType(), h.getTotalAmount(), h.getTransactionDate(),
                h.getNote(), h.getCreatedAt(), items
        );
    }
}
