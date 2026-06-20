package com.expensetracker.scheduler.entity;

import com.expensetracker.common.enums.RecurrenceFrequency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "recurring_transaction", schema = "scheduler_service")
@Getter @Setter @NoArgsConstructor
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(name = "next_execution_date", nullable = false)
    private LocalDate nextExecutionDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "header_template", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> headerTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "line_items_template", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> lineItemsTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
