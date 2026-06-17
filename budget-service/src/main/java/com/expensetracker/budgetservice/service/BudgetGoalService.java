package com.expensetracker.budgetservice.service;

import com.expensetracker.budgetservice.dto.BudgetGoalResponse;
import com.expensetracker.budgetservice.dto.CreateBudgetGoalRequest;
import com.expensetracker.budgetservice.entity.BudgetGoal;
import com.expensetracker.budgetservice.entity.BudgetPeriod;
import com.expensetracker.budgetservice.repository.BudgetGoalRepository;
import com.expensetracker.budgetservice.repository.BudgetPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetGoalService {

    private final BudgetGoalRepository goalRepo;
    private final BudgetPeriodRepository periodRepo;

    public BudgetGoalResponse create(UUID userId, CreateBudgetGoalRequest request) {
        BudgetGoal goal = new BudgetGoal();
        goal.setUserId(userId);
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setBudgetType(request.getBudgetType() != null ? request.getBudgetType() : "CUSTOM");
        goal = goalRepo.save(goal);

        LocalDate now = LocalDate.now();
        BudgetPeriod firstPeriod = new BudgetPeriod();
        firstPeriod.setBudgetGoal(goal);
        firstPeriod.setPeriodMonth(now.getMonthValue());
        firstPeriod.setPeriodYear(now.getYear());
        firstPeriod.setStartingBalance(request.getTargetAmount());
        periodRepo.save(firstPeriod);

        return BudgetGoalResponse.from(goal);
    }

    @Transactional(readOnly = true)
    public List<BudgetGoalResponse> listActive(UUID userId) {
        return goalRepo.findByUserIdAndIsActiveTrue(userId).stream()
                .map(BudgetGoalResponse::from).toList();
    }
}
