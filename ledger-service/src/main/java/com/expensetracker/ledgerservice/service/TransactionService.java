package com.expensetracker.ledgerservice.service;

import com.expensetracker.common.dto.PageResponse;
import com.expensetracker.common.enums.TransactionType;
import com.expensetracker.common.exception.LedgerImbalanceException;
import com.expensetracker.common.exception.ResourceNotFoundException;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.ledgerservice.client.BudgetServiceClient;
import com.expensetracker.ledgerservice.dto.*;
import com.expensetracker.ledgerservice.entity.TransactionHeader;
import com.expensetracker.ledgerservice.entity.TransactionLineItem;
import com.expensetracker.ledgerservice.repository.TransactionHeaderRepository;
import com.expensetracker.ledgerservice.repository.TransactionLineItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionHeaderRepository headerRepo;
    private final TransactionLineItemRepository lineItemRepo;
    private final BudgetServiceClient budgetClient;

    public TransactionResponse create(UUID userId, CreateTransactionRequest request) {
        validate(request);

        TransactionHeader header = new TransactionHeader();
        header.setUserId(userId);
        header.setSourceAccountId(request.getSourceAccountId());
        header.setDestAccountId(request.getDestAccountId());
        header.setTransactionType(request.getTransactionType());
        header.setTotalAmount(request.getTotalAmount());
        header.setTransactionDate(request.getTransactionDate());
        header.setNote(request.getNote());

        for (LineItemRequest li : request.getLineItems()) {
            TransactionLineItem item = new TransactionLineItem();
            item.setTransaction(header);
            item.setCategoryId(li.getCategoryId());
            item.setAmount(li.getAmount());
            item.setTagIds(li.getTagIds());
            header.getLineItems().add(item);
        }

        TransactionHeader saved = headerRepo.save(header);

        if (request.getTransactionType() == TransactionType.EXPENSE) {
            budgetClient.notifyExpense(
                    new ExpenseEventRequest(userId, saved.getTransactionDate(), saved.getTotalAmount()));
        }

        return TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id, UUID userId) {
        return TransactionResponse.from(
                headerRepo.findByIdAndUserId(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction", id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID userId, Integer month, Integer year, Pageable pageable) {
        Page<TransactionHeader> page;
        if (month != null && year != null) {
            Instant start = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant end = start.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
            page = headerRepo.findByUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThanOrderByTransactionDateDesc(
                    userId, start, end, pageable);
        } else {
            page = headerRepo.findByUserIdOrderByTransactionDateDesc(userId, pageable);
        }
        List<TransactionResponse> content = page.getContent().stream()
                .map(TransactionResponse::from).toList();
        return new PageResponse<>(content, page.getNumber(), page.getTotalPages(),
                page.getTotalElements(), page.getSize());
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownItem> getBreakdown(UUID userId, int month, int year, TransactionType type) {
        Instant start = LocalDate.of(year, month, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = start.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        return lineItemRepo.findBreakdown(userId, start, end, type);
    }

    public void delete(UUID id, UUID userId) {
        TransactionHeader header = headerRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        headerRepo.delete(header);
    }

    private void validate(CreateTransactionRequest req) {
        BigDecimal lineSum = req.getLineItems().stream()
                .map(LineItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lineSum.compareTo(req.getTotalAmount()) != 0) {
            throw new LedgerImbalanceException(
                    "Line items sum " + lineSum + " != total_amount " + req.getTotalAmount());
        }

        TransactionType type = req.getTransactionType();

        if (type == TransactionType.TRANSFER) {
            if (req.getDestAccountId() == null) {
                throw new ValidationException("TRANSFER requires dest_account_id");
            }
            boolean anyHasCategory = req.getLineItems().stream()
                    .anyMatch(li -> li.getCategoryId() != null);
            if (anyHasCategory) {
                throw new ValidationException("TRANSFER line items must have null category_id");
            }
        } else {
            boolean anyMissingCategory = req.getLineItems().stream()
                    .anyMatch(li -> li.getCategoryId() == null);
            if (anyMissingCategory) {
                throw new ValidationException(type + " line items must have a category_id");
            }
        }
    }
}
