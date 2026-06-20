package com.expensetracker.budget.service;

import com.expensetracker.budget.dto.AddCategoryMappingRequest;
import com.expensetracker.budget.dto.CategoryMappingResponse;
import com.expensetracker.budget.entity.BudgetGoal;
import com.expensetracker.budget.entity.BudgetGoalCategoryMapping;
import com.expensetracker.budget.entity.BudgetGoalCategoryMappingId;
import com.expensetracker.budget.repository.BudgetGoalCategoryMappingRepository;
import com.expensetracker.budget.repository.BudgetGoalRepository;
import com.expensetracker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetGoalCategoryMappingService {

    private final BudgetGoalRepository goalRepo;
    private final BudgetGoalCategoryMappingRepository mappingRepo;

    public CategoryMappingResponse addMapping(UUID goalId, AddCategoryMappingRequest request) {
        BudgetGoal goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetGoal", goalId));

        BudgetGoalCategoryMapping mapping = new BudgetGoalCategoryMapping(
                goal, request.getCategoryId(), request.isDefault());
        mapping = mappingRepo.save(mapping);
        return CategoryMappingResponse.from(mapping);
    }

    @Transactional(readOnly = true)
    public List<CategoryMappingResponse> listMappings(UUID goalId) {
        return mappingRepo.findByBudgetGoalId(goalId).stream()
                .map(CategoryMappingResponse::from).toList();
    }

    public void deleteMapping(UUID goalId, UUID categoryId) {
        BudgetGoalCategoryMappingId id = new BudgetGoalCategoryMappingId(goalId, categoryId);
        if (!mappingRepo.existsById(id)) {
            throw new ResourceNotFoundException("CategoryMapping", categoryId);
        }
        mappingRepo.deleteById(id);
    }
}
