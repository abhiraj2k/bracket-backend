package com.expensetracker.metadataservice.service;

import com.expensetracker.common.exception.ValidationException;
import com.expensetracker.metadataservice.dto.CreateTagRequest;
import com.expensetracker.metadataservice.dto.TagResponse;
import com.expensetracker.metadataservice.entity.Tag;
import com.expensetracker.metadataservice.entity.UserTagMapping;
import com.expensetracker.metadataservice.repository.TagRepository;
import com.expensetracker.metadataservice.repository.UserTagMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepo;
    private final UserTagMappingRepository userTagMappingRepo;

    @Cacheable(value = "user-tags", key = "#userId")
    @Transactional(readOnly = true)
    public List<TagResponse> getUserTags(UUID userId) {
        List<Tag> result = new ArrayList<>(tagRepo.findByIsGlobalTrue());

        List<UUID> personalTagIds = userTagMappingRepo.findByIdUserId(userId).stream()
                .map(m -> m.getId().getTagId())
                .toList();
        if (!personalTagIds.isEmpty()) {
            tagRepo.findAllById(personalTagIds).forEach(result::add);
        }

        return result.stream().map(TagResponse::from).toList();
    }

    @CacheEvict(value = "user-tags", key = "#userId")
    public TagResponse createPersonalTag(UUID userId, CreateTagRequest request) {
        if (tagRepo.existsByName(request.getName())) {
            throw new ValidationException("Tag name already exists: " + request.getName());
        }

        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setIsGlobal(false);
        tag = tagRepo.save(tag);

        userTagMappingRepo.save(new UserTagMapping(userId, tag));
        return TagResponse.from(tag);
    }
}
