package com.expensetracker.metadataservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_tag_mapping", schema = "metadata_service")
@Getter @Setter @NoArgsConstructor
public class UserTagMapping {

    @EmbeddedId
    private UserTagMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private Tag tag;

    public UserTagMapping(UUID userId, Tag tag) {
        this.id = new UserTagMappingId(userId, tag.getId());
        this.tag = tag;
    }

    private UUID getUserId() { return id.getUserId(); }
}
