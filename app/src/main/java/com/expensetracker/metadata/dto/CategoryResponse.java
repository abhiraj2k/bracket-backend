package com.expensetracker.metadata.dto;

import com.expensetracker.common.enums.CategoryType;
import com.expensetracker.metadata.entity.Category;
import com.expensetracker.metadata.entity.UserCategoryMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Getter @AllArgsConstructor
public class CategoryResponse implements Serializable {
    private UUID id;
    private UUID parentCategoryId;
    private String name;
    private CategoryType categoryType;
    private String customAlias;
    private Boolean isActive;

    public static CategoryResponse from(UserCategoryMapping mapping) {
        Category c = mapping.getCategory();
        return new CategoryResponse(
                c.getId(), c.getParentCategoryId(), c.getName(), c.getCategoryType(),
                mapping.getCustomAlias(), mapping.getIsActive()
        );
    }

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getParentCategoryId(), c.getName(),
                c.getCategoryType(), null, true);
    }
}
