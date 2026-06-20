package com.expensetracker.ledger.repository;

import com.expensetracker.ledger.entity.TransactionHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransactionHeaderRepository extends JpaRepository<TransactionHeader, UUID> {
    Page<TransactionHeader> findByUserIdOrderByTransactionDateDesc(UUID userId, Pageable pageable);
    Page<TransactionHeader> findByUserIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThanOrderByTransactionDateDesc(
            UUID userId, Instant start, Instant end, Pageable pageable);
    Optional<TransactionHeader> findByIdAndUserId(UUID id, UUID userId);
}
