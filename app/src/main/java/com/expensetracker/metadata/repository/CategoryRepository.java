package com.expensetracker.metadata.repository;

import com.expensetracker.metadata.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByIsGlobalTrue();
}
