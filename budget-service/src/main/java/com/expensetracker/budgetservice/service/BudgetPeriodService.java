package com.expensetracker.budgetservice.service;

import com.expensetracker.budgetservice.dto.BudgetPeriodResponse;
import com.expensetracker.budgetservice.dto.ExpenseEventRequest;
import com.expensetracker.budgetservice.entity.BudgetPeriod;
import com.expensetracker.budgetservice.repository.BudgetPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BudgetPeriodService {

    private final BudgetPeriodRepository periodRepo;

    @Transactional(readOnly = true)
    public List<BudgetPeriodResponse> getCurrentPeriods(UUID userId) {
        LocalDate now = LocalDate.now();
        return periodRepo.findByBudgetGoal_UserIdAndPeriodMonthAndPeriodYear(
                        userId, now.getMonthValue(), now.getYear()).stream()
                .map(BudgetPeriodResponse::from)
                .toList();
    }

    public void handleExpenseEvent(ExpenseEventRequest event) {
        LocalDate txDate = event.getTransactionDate()
                .atZone(ZoneOffset.UTC).toLocalDate();

        List<BudgetPeriod> periods = periodRepo.findByBudgetGoal_UserIdAndPeriodMonthAndPeriodYear(
                event.getUserId(), txDate.getMonthValue(), txDate.getYear());

        if (periods.isEmpty()) {
            log.warn("No active budget periods for user {} in {}/{}", event.getUserId(),
                    txDate.getMonthValue(), txDate.getYear());
            return;
        }

        // distribute expense equally across active periods for the month
        for (BudgetPeriod period : periods) {
            period.setSpentAmount(period.getSpentAmount().add(event.getTotalAmount()));
        }
        periodRepo.saveAll(periods);
    }
}
