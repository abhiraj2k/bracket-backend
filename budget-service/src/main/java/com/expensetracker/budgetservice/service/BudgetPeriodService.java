package com.expensetracker.budgetservice.service;

import com.expensetracker.budgetservice.dto.BudgetPeriodResponse;
import com.expensetracker.budgetservice.dto.ExpenseEventRequest;
import com.expensetracker.budgetservice.dto.LineItemExpense;
import com.expensetracker.budgetservice.entity.BudgetGoalCategoryMapping;
import com.expensetracker.budgetservice.entity.BudgetPeriod;
import com.expensetracker.budgetservice.repository.BudgetGoalCategoryMappingRepository;
import com.expensetracker.budgetservice.repository.BudgetPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BudgetPeriodService {

    private final BudgetPeriodRepository periodRepo;
    private final BudgetGoalCategoryMappingRepository mappingRepo;

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
        int month = txDate.getMonthValue();
        int year = txDate.getYear();

        for (LineItemExpense lineItem : event.getLineItems()) {
            UUID goalId = resolveGoalId(lineItem);
            if (goalId == null) {
                log.warn("No goal for categoryId={}, skipping", lineItem.getCategoryId());
                continue;
            }

            Optional<BudgetPeriod> periodOpt = periodRepo
                    .findByBudgetGoalIdAndPeriodMonthAndPeriodYear(goalId, month, year);

            if (periodOpt.isEmpty()) {
                log.warn("No budget period for goalId={} {}/{}", goalId, month, year);
                continue;
            }

            BudgetPeriod period = periodOpt.get();
            period.setSpentAmount(period.getSpentAmount().add(lineItem.getAmount()));
            periodRepo.save(period);
        }
    }

    private UUID resolveGoalId(LineItemExpense lineItem) {
        if (lineItem.getBudgetGoalId() != null) {
            return lineItem.getBudgetGoalId();
        }
        if (lineItem.getCategoryId() != null) {
            return mappingRepo.findFirstByIdCategoryIdAndIsDefaultTrue(lineItem.getCategoryId())
                    .map(BudgetGoalCategoryMapping::getId)
                    .map(id -> id.getBudgetGoalId())
                    .orElse(null);
        }
        return null;
    }
}
