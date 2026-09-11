package com.lifelately.service;

import com.lifelately.dao.EntryDAO;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.model.Tag;
import com.lifelately.util.ValidationUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntryService {
    public record Insights(int totalEntries, int entriesThisMonth, int currentStreak, int longestStreak,
                           String mostCommonMood, Map<String, Long> moodCounts, List<String> topTags) { }

    private final EntryDAO entryDAO;
    private final TagService tagService;

    public EntryService(EntryDAO entryDAO, TagService tagService) {
        this.entryDAO = entryDAO;
        this.tagService = tagService;
    }

    public List<Entry> getAllEntries() {
        return entryDAO.findAll();
    }

    public Optional<Entry> getEntry(long id) {
        return entryDAO.findById(id);
    }

    public List<Entry> search(String query, Mood mood, Tag tag, boolean favoritesOnly, String sort) {
        return entryDAO.search(query, mood == null ? null : mood.id(), tag == null ? null : tag.id(),
                favoritesOnly, sort);
    }

    public Entry save(Long id, String title, String content, Mood mood, LocalDate date,
                      boolean favorite, String tags) {
        Entry entry = id == null ? new Entry() : getEntry(id)
                .orElseThrow(() -> new IllegalArgumentException("That journal entry no longer exists."));
        entry.setTitle(ValidationUtils.requireText(title, "Title"));
        entry.setContent(ValidationUtils.requireText(content, "Journal entry"));
        if (mood == null) throw new IllegalArgumentException("Choose a mood.");
        if (date == null) throw new IllegalArgumentException("Choose a date.");
        entry.setMood(mood);
        entry.setEntryDate(date);
        entry.setFavorite(favorite);
        entry.setTags(tagService.resolveTags(tags));
        return id == null ? entryDAO.insert(entry) : entryDAO.update(entry);
    }

    public void delete(long id) {
        entryDAO.softDelete(id);
    }

    public void restore(long id) {
        entryDAO.restore(id);
    }

    public Insights getInsights() {
        List<Entry> entries = getAllEntries();
        YearMonth currentMonth = YearMonth.now();
        int entriesThisMonth = (int) entries.stream()
                .filter(entry -> YearMonth.from(entry.getEntryDate()).equals(currentMonth)).count();
        Map<String, Long> moodCounts = entries.stream().collect(Collectors.groupingBy(
                entry -> entry.getMood().name(), Collectors.counting()));
        String commonMood = moodCounts.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("—");
        Map<String, Long> tagCounts = entries.stream().flatMap(entry -> entry.getTags().stream())
                .map(Tag::name).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<String> topTags = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .map(Map.Entry::getKey).toList();
        return new Insights(entries.size(), entriesThisMonth, currentStreak(entries), longestStreak(entries),
                commonMood, moodCounts, topTags);
    }

    private int currentStreak(List<Entry> entries) {
        Set<LocalDate> dates = entries.stream().map(Entry::getEntryDate).collect(Collectors.toSet());
        LocalDate cursor = LocalDate.now();
        if (!dates.contains(cursor)) cursor = cursor.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int longestStreak(List<Entry> entries) {
        List<LocalDate> dates = entries.stream().map(Entry::getEntryDate).distinct().sorted().toList();
        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate date : dates) {
            current = previous != null && date.equals(previous.plusDays(1)) ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = date;
        }
        return longest;
    }
}
