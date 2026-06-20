package com.expensetracker.scheduler.repository;

import com.expensetracker.scheduler.entity.RecurringExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecurringExecutionLogRepository extends JpaRepository<RecurringExecutionLog, UUID> {
}
