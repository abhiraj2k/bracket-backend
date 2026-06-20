package com.expensetracker.budget.service;

import com.expensetracker.budget.dto.BudgetGoalResponse;
import com.expensetracker.budget.dto.CreateBudgetGoalRequest;
import com.expensetracker.budget.entity.BudgetGoal;
import com.expensetracker.budget.entity.BudgetPeriod;
import com.expensetracker.budget.repository.BudgetGoalRepository;
import com.expensetracker.budget.repository.BudgetPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expensetracker.common.exception.ResourceNotFoundException;

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

    public void delete(UUID userId, UUID goalId) {
        BudgetGoal goal = goalRepo.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetGoal", goalId));
        goal.setIsActive(false);
        goalRepo.save(goal);
    }

    @Transactional(readOnly = true)
    public List<BudgetGoalResponse> listActive(UUID userId) {
        return goalRepo.findByUserIdAndIsActiveTrue(userId).stream()
                .map(BudgetGoalResponse::from).toList();
    }
}
