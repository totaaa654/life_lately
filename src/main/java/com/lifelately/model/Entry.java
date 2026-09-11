package com.lifelately.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Entry {
    private long id;
    private String title;
    private String content;
    private Mood mood;
    private LocalDate entryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean favorite;
    private LocalDateTime deletedAt;
    private List<Tag> tags = new ArrayList<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Mood getMood() { return mood; }
    public void setMood(Mood mood) { this.mood = mood; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public List<Tag> getTags() { return List.copyOf(tags); }
    public void setTags(List<Tag> tags) { this.tags = new ArrayList<>(tags == null ? List.of() : tags); }
}
