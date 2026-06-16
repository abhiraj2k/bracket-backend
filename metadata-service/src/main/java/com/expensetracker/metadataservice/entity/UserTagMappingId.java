package com.expensetracker.metadataservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserTagMappingId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tag_id")
    private UUID tagId;
}
