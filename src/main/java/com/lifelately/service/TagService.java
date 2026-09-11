package com.lifelately.service;

import com.lifelately.dao.TagDAO;
import com.lifelately.model.Tag;

import java.util.Arrays;
import java.util.List;

public final class TagService {
    private final TagDAO tagDAO;
    private final AuthService authService;

    public TagService(TagDAO tagDAO, AuthService authService) {
        this.tagDAO = tagDAO;
        this.authService = authService;
    }

    public List<Tag> getTags() {
        long userId = authService.currentUser()
                .orElseThrow(() -> new IllegalStateException("Sign in to access journal tags."))
                .id();
        return tagDAO.findAllForUser(userId);
    }

    public List<Tag> resolveTags(String commaSeparatedTags) {
        if (commaSeparatedTags == null || commaSeparatedTags.isBlank()) return List.of();
        return Arrays.stream(commaSeparatedTags.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .map(name -> name.length() > 40 ? name.substring(0, 40) : name)
                .distinct()
                .limit(10)
                .map(tagDAO::findOrCreate)
                .toList();
    }
}
