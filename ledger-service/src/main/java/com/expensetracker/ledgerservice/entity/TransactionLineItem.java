package com.expensetracker.ledgerservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "transaction_line_item", schema = "ledger_service")
@Getter @Setter @NoArgsConstructor
public class TransactionLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionHeader transaction;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "budget_goal_id")
    private UUID budgetGoalId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ElementCollection
    @CollectionTable(
            name = "transaction_tag_mapping",
            schema = "ledger_service",
            joinColumns = @JoinColumn(name = "line_item_id")
    )
    @Column(name = "tag_id")
    private Set<UUID> tagIds = new HashSet<>();
}
