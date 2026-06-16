package com.expensetracker.metadataservice.dto;

import com.expensetracker.metadataservice.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter @AllArgsConstructor
public class TagResponse {
    private UUID id;
    private String name;
    private Boolean isGlobal;

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getIsGlobal());
    }
}
