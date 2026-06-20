package com.expensetracker.metadata.dto;

import com.expensetracker.metadata.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter @AllArgsConstructor
public class TagResponse implements Serializable {
    private UUID id;
    private String name;
    private Boolean isGlobal;

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getIsGlobal());
    }
}
