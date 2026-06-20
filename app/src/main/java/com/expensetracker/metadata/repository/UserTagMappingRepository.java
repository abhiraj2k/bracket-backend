package com.expensetracker.metadata.repository;

import com.expensetracker.metadata.entity.UserTagMapping;
import com.expensetracker.metadata.entity.UserTagMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTagMappingRepository extends JpaRepository<UserTagMapping, UserTagMappingId> {
    List<UserTagMapping> findByIdUserId(UUID userId);
}
