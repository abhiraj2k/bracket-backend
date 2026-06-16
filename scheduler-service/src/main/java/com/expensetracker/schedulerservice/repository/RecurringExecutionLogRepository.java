package com.expensetracker.schedulerservice.repository;

import com.expensetracker.schedulerservice.entity.RecurringExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecurringExecutionLogRepository extends JpaRepository<RecurringExecutionLog, UUID> {
}
