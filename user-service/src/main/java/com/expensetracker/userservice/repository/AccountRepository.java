package com.expensetracker.userservice.repository;

import com.expensetracker.userservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByUserIdAndIsActiveTrue(UUID userId);
}
