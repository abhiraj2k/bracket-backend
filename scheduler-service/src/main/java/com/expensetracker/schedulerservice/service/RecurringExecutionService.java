package com.expensetracker.schedulerservice.service;

import com.expensetracker.common.enums.ExecutionStatus;
import com.expensetracker.common.enums.RecurrenceFrequency;
import com.expensetracker.schedulerservice.client.LedgerServiceClient;
import com.expensetracker.schedulerservice.entity.RecurringExecutionLog;
import com.expensetracker.schedulerservice.entity.RecurringTransaction;
import com.expensetracker.schedulerservice.repository.RecurringExecutionLogRepository;
import com.expensetracker.schedulerservice.repository.RecurringTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExecutionService {

    private final RecurringTransactionRepository recurringRepo;
    private final RecurringExecutionLogRepository logRepo;
    private final LedgerServiceClient ledgerClient;

    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(name = "recurringTransactionJob", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void executeRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> due = recurringRepo.findByIsActiveTrueAndNextExecutionDateLessThanEqual(today);
        log.info("Recurring job: {} transactions due for {}", due.size(), today);

        for (RecurringTransaction rt : due) {
            executeOne(rt, today);
        }
    }

    private void executeOne(RecurringTransaction rt, LocalDate today) {
        RecurringExecutionLog logEntry = new RecurringExecutionLog();
        logEntry.setRecurringTransaction(rt);

        try {
            Map<String, Object> payload = buildPayload(rt, today);
            ledgerClient.createTransaction(rt.getUserId(), payload);

            logEntry.setStatus(ExecutionStatus.SUCCESS);
            rt.setNextExecutionDate(advanceDate(rt.getNextExecutionDate(), rt.getFrequency()));
            recurringRepo.save(rt);

            log.info("Executed recurring txn {} — next: {}", rt.getId(), rt.getNextExecutionDate());
        } catch (Exception e) {
            logEntry.setStatus(ExecutionStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
            log.error("Failed recurring txn {}: {}", rt.getId(), e.getMessage());
        }

        logRepo.save(logEntry);
    }

    private Map<String, Object> buildPayload(RecurringTransaction rt, LocalDate today) {
        Map<String, Object> payload = new HashMap<>(rt.getHeaderTemplate());
        payload.put("transactionDate", today.atStartOfDay().toString() + "Z");
        payload.put("lineItems", rt.getLineItemsTemplate());
        return payload;
    }

    private LocalDate advanceDate(LocalDate date, RecurrenceFrequency freq) {
        return switch (freq) {
            case DAILY   -> date.plusDays(1);
            case WEEKLY  -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY  -> date.plusYears(1);
        };
    }
}
