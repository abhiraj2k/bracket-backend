package com.expensetracker.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when line item amounts do not sum to the transaction header total_amount.
 * Always triggers a full @Transactional rollback.
 */
public class LedgerImbalanceException extends ApiException {
    public LedgerImbalanceException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
