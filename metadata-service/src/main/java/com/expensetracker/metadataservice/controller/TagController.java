package com.expensetracker.metadataservice.controller;

import com.expensetracker.common.dto.ApiResponse;
import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.metadataservice.dto.CreateTagRequest;
import com.expensetracker.metadataservice.dto.TagResponse;
import com.expensetracker.metadataservice.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ApiResponse<List<TagResponse>> list(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(tagService.getUserTags(parseUserId(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TagResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateTagRequest request) {
        return ApiResponse.ok("Tag created", tagService.createPersonalTag(parseUserId(userId), request));
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid X-User-Id header: " + userId);
        }
    }
}
