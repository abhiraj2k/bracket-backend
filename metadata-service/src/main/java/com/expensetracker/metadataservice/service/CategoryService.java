package com.expensetracker.metadataservice.service;

import com.expensetracker.common.exception.ResourceNotFoundException;
import com.expensetracker.metadataservice.dto.CategoryResponse;
import com.expensetracker.metadataservice.dto.UpdateCategoryAliasRequest;
import com.expensetracker.metadataservice.entity.UserCategoryMapping;
import com.expensetracker.metadataservice.entity.UserCategoryMappingId;
import com.expensetracker.metadataservice.repository.CategoryRepository;
import com.expensetracker.metadataservice.repository.UserCategoryMappingRepository;
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

    @Cacheable(value = "user-categories", key = "#userId")
    public List<CategoryResponse> getUserCategories(UUID userId) {
        if (!mappingRepo.existsByIdUserId(userId)) {
            seedDefaultMappings(userId);
        }
        return mappingRepo.findByIdUserId(userId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @CacheEvict(value = "user-categories", key = "#userId")
    public CategoryResponse updateAlias(UUID userId, UUID categoryId, UpdateCategoryAliasRequest request) {
        UserCategoryMappingId pk = new UserCategoryMappingId(userId, categoryId);
        UserCategoryMapping mapping = mappingRepo.findById(pk)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryMapping", categoryId));
        mapping.setCustomAlias(request.getCustomAlias());
        return CategoryResponse.from(mappingRepo.save(mapping));
    }

    private void seedDefaultMappings(UUID userId) {
        List<UserCategoryMapping> defaults = categoryRepo.findByIsGlobalTrue().stream()
                .map(c -> new UserCategoryMapping(userId, c))
                .toList();
        mappingRepo.saveAll(defaults);
    }
}
