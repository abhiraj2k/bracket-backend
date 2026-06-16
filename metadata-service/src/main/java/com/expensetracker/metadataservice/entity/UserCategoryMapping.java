package com.expensetracker.metadataservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_category_mapping", schema = "metadata_service")
@Getter @Setter @NoArgsConstructor
public class UserCategoryMapping {

    @EmbeddedId
    private UserCategoryMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "custom_alias", length = 100)
    private String customAlias;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public UserCategoryMapping(UUID userId, Category category) {
        this.id = new UserCategoryMappingId(userId, category.getId());
        this.category = category;
    }

    private UUID getUserId() { return id.getUserId(); }
}
