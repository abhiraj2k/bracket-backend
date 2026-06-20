package com.expensetracker.scheduler.service;

import com.expensetracker.common.exception.ResourceNotFoundException;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.scheduler.dto.CreateRecurringTransactionRequest;
import com.expensetracker.scheduler.dto.RecurringTransactionResponse;
import com.expensetracker.scheduler.entity.RecurringTransaction;
import com.expensetracker.scheduler.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepo;

    public RecurringTransactionResponse create(UUID userId, CreateRecurringTransactionRequest request) {
        RecurringTransaction rt = new RecurringTransaction();
        rt.setUserId(userId);
        rt.setFrequency(request.getFrequency());
        rt.setNextExecutionDate(request.getNextExecutionDate());
        rt.setHeaderTemplate(request.getHeaderTemplate());
        rt.setLineItemsTemplate(request.getLineItemsTemplate());
        return RecurringTransactionResponse.from(recurringRepo.save(rt));
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> listActive(UUID userId) {
        return recurringRepo.findByUserIdAndIsActiveTrue(userId).stream()
                .map(RecurringTransactionResponse::from).toList();
    }

    public void deactivate(UUID id, UUID userId) {
        RecurringTransaction rt = recurringRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RecurringTransaction", id));
        if (!rt.getUserId().equals(userId)) {
            throw new ValidationException("RecurringTransaction does not belong to user");
        }
        rt.setIsActive(false);
        recurringRepo.save(rt);
    }
}
