package com.expensetracker.metadataservice.repository;

import com.expensetracker.metadataservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByIsGlobalTrue();
}
