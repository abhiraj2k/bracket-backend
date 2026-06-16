package com.expensetracker.budgetservice.service;

import com.expensetracker.budgetservice.entity.BudgetGoal;
import com.expensetracker.budgetservice.entity.BudgetPeriod;
import com.expensetracker.budgetservice.repository.BudgetGoalRepository;
import com.expensetracker.budgetservice.repository.BudgetPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolloverService {

    private final BudgetGoalRepository goalRepo;
    private final BudgetPeriodRepository periodRepo;

    // 00:01 on the 1st of every month
    @Scheduled(cron = "0 1 0 1 * ?")
    @Transactional
    public void performRollover() {
        LocalDate now = LocalDate.now();
        LocalDate prev = now.minusMonths(1);

        log.info("Starting budget rollover for {}/{}", now.getMonthValue(), now.getYear());

        for (BudgetGoal goal : goalRepo.findByIsActiveTrue()) {
            try {
                rolloverGoal(goal, prev, now);
            } catch (Exception e) {
                log.error("Rollover failed for goal {}: {}", goal.getId(), e.getMessage());
            }
        }

        log.info("Budget rollover complete");
    }

    private void rolloverGoal(BudgetGoal goal, LocalDate prev, LocalDate now) {
        Optional<BudgetPeriod> prevPeriod = periodRepo.findByBudgetGoalIdAndPeriodMonthAndPeriodYear(
                goal.getId(), prev.getMonthValue(), prev.getYear());

        // net_balance = starting_balance - spent_amount (positive=surplus, negative=deficit)
        BigDecimal netBalance = prevPeriod
                .map(p -> p.getStartingBalance().subtract(p.getSpentAmount()))
                .orElse(BigDecimal.ZERO);

        BigDecimal newStartingBalance = goal.getTargetAmount().add(netBalance);
        // floor at zero — starting balance can't be negative
        if (newStartingBalance.compareTo(BigDecimal.ZERO) < 0) {
            newStartingBalance = BigDecimal.ZERO;
        }

        boolean alreadyExists = periodRepo.findByBudgetGoalIdAndPeriodMonthAndPeriodYear(
                goal.getId(), now.getMonthValue(), now.getYear()).isPresent();
        if (alreadyExists) {
            log.debug("Period already exists for goal {} in {}/{}", goal.getId(),
                    now.getMonthValue(), now.getYear());
            return;
        }

        BudgetPeriod newPeriod = new BudgetPeriod();
        newPeriod.setBudgetGoal(goal);
        newPeriod.setPeriodMonth(now.getMonthValue());
        newPeriod.setPeriodYear(now.getYear());
        newPeriod.setStartingBalance(newStartingBalance);
        periodRepo.save(newPeriod);

        log.info("Created period for goal {} — starting_balance={}", goal.getId(), newStartingBalance);
    }
}
