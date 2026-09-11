package com.lifelately.service;

import com.lifelately.dao.TagDAO;
import com.lifelately.model.Tag;

import java.util.Arrays;
import java.util.List;

public final class TagService {
    private final TagDAO tagDAO;

    public TagService(TagDAO tagDAO) {
        this.tagDAO = tagDAO;
    }

    public List<Tag> getTags() {
        return tagDAO.findAll();
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
