package com.expensetracker.scheduler.entity;

import com.expensetracker.common.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recurring_execution_log", schema = "scheduler_service")
@Getter @Setter @NoArgsConstructor
public class RecurringExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recurring_id", nullable = false)
    private RecurringTransaction recurringTransaction;

    @CreationTimestamp
    @Column(name = "execution_date", nullable = false, updatable = false)
    private Instant executionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
