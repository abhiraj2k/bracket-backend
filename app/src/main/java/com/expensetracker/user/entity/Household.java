package com.expensetracker.user.entity;

import com.expensetracker.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "household", schema = "user_service")
@Getter @Setter @NoArgsConstructor
public class Household extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;
}
