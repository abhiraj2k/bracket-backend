package com.expensetracker.ledger.repository;

import com.expensetracker.ledger.dto.CategoryBreakdownItem;
import com.expensetracker.ledger.entity.TransactionLineItem;
import com.expensetracker.common.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionLineItemRepository extends JpaRepository<TransactionLineItem, UUID> {

    @Query("""
        SELECT li.categoryId AS categoryId, SUM(li.amount) AS total
        FROM TransactionLineItem li
        WHERE li.transaction.userId = :userId
          AND li.transaction.transactionDate >= :start
          AND li.transaction.transactionDate < :end
          AND li.transaction.transactionType = :type
          AND li.categoryId IS NOT NULL
        GROUP BY li.categoryId
        ORDER BY SUM(li.amount) DESC
        """)
    List<CategoryBreakdownItem> findBreakdown(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("type") TransactionType type
    );
}
