package com.expensetracker.user.entity;

import com.expensetracker.common.entity.AuditableEntity;
import com.expensetracker.common.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "account", schema = "user_service")
@Getter @Setter @NoArgsConstructor
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "INR";

    @Column(name = "billing_start_day")
    private Integer billingStartDay;

    @Column(name = "billing_end_day")
    private Integer billingEndDay;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
