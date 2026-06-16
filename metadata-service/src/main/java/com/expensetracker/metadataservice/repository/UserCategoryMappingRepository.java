package com.expensetracker.metadataservice.repository;

import com.expensetracker.metadataservice.entity.UserCategoryMapping;
import com.expensetracker.metadataservice.entity.UserCategoryMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCategoryMappingRepository extends JpaRepository<UserCategoryMapping, UserCategoryMappingId> {
    List<UserCategoryMapping> findByIdUserId(UUID userId);
    boolean existsByIdUserId(UUID userId);
}
