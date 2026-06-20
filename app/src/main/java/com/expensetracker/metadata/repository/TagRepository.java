package com.expensetracker.metadata.repository;

import com.expensetracker.metadata.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByIsGlobalTrue();
    boolean existsByName(String name);
    Optional<Tag> findByName(String name);
}
