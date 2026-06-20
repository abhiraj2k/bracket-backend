package com.expensetracker.metadata.repository;

import com.expensetracker.metadata.entity.UserCategoryMapping;
import com.expensetracker.metadata.entity.UserCategoryMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCategoryMappingRepository extends JpaRepository<UserCategoryMapping, UserCategoryMappingId> {
    List<UserCategoryMapping> findByIdUserId(UUID userId);
    boolean existsByIdUserId(UUID userId);
}
