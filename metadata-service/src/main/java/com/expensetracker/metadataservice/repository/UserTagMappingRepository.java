package com.expensetracker.metadataservice.repository;

import com.expensetracker.metadataservice.entity.UserTagMapping;
import com.expensetracker.metadataservice.entity.UserTagMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserTagMappingRepository extends JpaRepository<UserTagMapping, UserTagMappingId> {
    List<UserTagMapping> findByIdUserId(UUID userId);
}
