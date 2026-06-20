package com.expensetracker.metadata.service;

import com.expensetracker.common.exception.ResourceNotFoundException;
import com.expensetracker.metadata.dto.CategoryResponse;
import com.expensetracker.metadata.dto.CreateCategoryRequest;
import com.expensetracker.metadata.dto.UpdateCategoryAliasRequest;
import com.expensetracker.metadata.entity.Category;
import com.expensetracker.metadata.entity.UserCategoryMapping;
import com.expensetracker.metadata.entity.UserCategoryMappingId;
import com.expensetracker.metadata.repository.CategoryRepository;
import com.expensetracker.metadata.repository.UserCategoryMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final UserCategoryMappingRepository mappingRepo;

    @Cacheable(value = "user-categories", key = "#p0")
    public List<CategoryResponse> getUserCategories(UUID userId) {
        if (!mappingRepo.existsByIdUserId(userId)) {
            seedDefaultMappings(userId);
        }
        return mappingRepo.findByIdUserId(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @CacheEvict(value = "user-categories", key = "#p0")
    public CategoryResponse updateAlias(UUID userId, UUID categoryId, UpdateCategoryAliasRequest request) {
        UserCategoryMappingId pk = new UserCategoryMappingId(userId, categoryId);
        UserCategoryMapping mapping = mappingRepo.findById(pk)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryMapping", categoryId));
        mapping.setCustomAlias(request.getCustomAlias());
        return CategoryResponse.from(mappingRepo.save(mapping));
    }

    @CacheEvict(value = "user-categories", key = "#p0")
    public CategoryResponse createPersonalCategory(UUID userId, CreateCategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setCategoryType(request.getCategoryType());
        category.setParentCategoryId(request.getParentCategoryId());
        category.setIsGlobal(false);
        category = categoryRepo.save(category);

        UserCategoryMapping mapping = new UserCategoryMapping(userId, category);
        mappingRepo.save(mapping);
        return CategoryResponse.from(mapping);
    }

    private void seedDefaultMappings(UUID userId) {
        List<UserCategoryMapping> defaults = categoryRepo.findByIsGlobalTrue().stream()
                .map(c -> new UserCategoryMapping(userId, c))
                .toList();
        mappingRepo.saveAll(defaults);
    }
}
