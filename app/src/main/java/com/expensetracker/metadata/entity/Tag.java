package com.expensetracker.metadata.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tag", schema = "metadata_service")
@Getter @Setter @NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = true;
}
