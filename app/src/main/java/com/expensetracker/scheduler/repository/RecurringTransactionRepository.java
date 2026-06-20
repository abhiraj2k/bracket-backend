package com.expensetracker.scheduler.repository;

import com.expensetracker.scheduler.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {
    List<RecurringTransaction> findByUserIdAndIsActiveTrue(UUID userId);
    List<RecurringTransaction> findByIsActiveTrueAndNextExecutionDateLessThanEqual(LocalDate date);
}
