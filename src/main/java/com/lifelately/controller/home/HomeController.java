package com.lifelately.controller.home;

import com.lifelately.config.AppConfig;
import com.lifelately.model.Entry;
import com.lifelately.navigation.NavigationManager;
import com.lifelately.navigation.View;
import com.lifelately.service.EntryService;
import com.lifelately.util.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public final class HomeController {
    @FXML private Label greetingLabel;
    @FXML private Label todayLabel;
    @FXML private Label streakValue;
    @FXML private Label monthValue;
    @FXML private Label moodValue;
    @FXML private Label monthLabel;
    @FXML private VBox recentEntries;
    @FXML private VBox emptyState;
    @FXML private VBox databaseNotice;
    @FXML private HBox calendarDays;
    @FXML private ToggleGroup moodGroup;

    private final AppConfig config = AppConfig.getInstance();

    @FXML
    private void initialize() {
        greetingLabel.setText(DateUtils.greeting() + ", " + friendlyName() + "!");
        todayLabel.setText(DateUtils.format(LocalDate.now(), "EEEE, MMMM d, yyyy"));
        monthLabel.setText(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        buildCalendarPreview();
        if (!config.isDatabaseReady()) {
            databaseNotice.setVisible(true);
            databaseNotice.setManaged(true);
            recentEntries.setVisible(false);
            recentEntries.setManaged(false);
            return;
        }
        refreshData();
    }

    @FXML
    private void writeToday() {
        if (!config.isDatabaseReady()) return;
        Toggle selected = moodGroup.getSelectedToggle();
        String mood = selected == null ? null : selected.getUserData().toString();
        NavigationManager.getInstance().openEditor(null, mood);
    }

    @FXML
    private void openJournal() {
        NavigationManager.getInstance().navigate(View.JOURNAL);
    }

    private void refreshData() {
        EntryService.Insights insights = config.entryService().getInsights();
        streakValue.setText(Integer.toString(insights.currentStreak()));
        monthValue.setText(Integer.toString(insights.entriesThisMonth()));
        moodValue.setText(insights.mostCommonMood());

        List<Entry> entries = config.entryService().getAllEntries().stream().limit(3).toList();
        emptyState.setVisible(entries.isEmpty());
        emptyState.setManaged(entries.isEmpty());
        recentEntries.getChildren().removeIf(node -> node != emptyState);
        for (Entry entry : entries) recentEntries.getChildren().add(createRecentRow(entry));
    }

    private HBox createRecentRow(Entry entry) {
        Label month = new Label(entry.getEntryDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase());
        month.getStyleClass().add("memory-date-month");
        Label day = new Label(Integer.toString(entry.getEntryDate().getDayOfMonth()));
        day.getStyleClass().add("memory-date-day");
        VBox dateTile = new VBox(1, month, day);
        dateTile.getStyleClass().add("memory-date-tile");
        Label title = new Label(entry.getTitle());
        title.getStyleClass().add("recent-title");
        String content = entry.getContent().replaceAll("\\s+", " ").trim();
        if (content.length() > 68) content = content.substring(0, 68) + "…";
        Label preview = new Label(content);
        preview.getStyleClass().add("memory-preview");
        Label mood = new Label(entry.getMood().name());
        mood.getStyleClass().addAll("mini-mood", "mood-" + entry.getMood().name().toLowerCase());
        VBox copy = new VBox(3, title, preview, mood);
        HBox.setHgrow(copy, javafx.scene.layout.Priority.ALWAYS);
        HBox row = new HBox(14, dateTile, copy);
        row.getStyleClass().add("recent-entry-row");
        row.setOnMouseClicked(event -> NavigationManager.getInstance().openDetails(entry.getId()));
        return row;
    }

    private void buildCalendarPreview() {
        LocalDate today = LocalDate.now();
        for (int offset = -3; offset <= 3; offset++) {
            LocalDate date = today.plusDays(offset);
            Label day = new Label(date.getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    + "\n" + date.getDayOfMonth());
            day.getStyleClass().add("mini-calendar-day");
            if (offset == 0) day.getStyleClass().add("mini-calendar-today");
            calendarDays.getChildren().add(day);
        }
    }

    private String friendlyName() {
        String user = System.getProperty("user.name", "friend").strip();
        if (user.isBlank()) return "friend";
        String first = user.split("[ ._-]")[0].toLowerCase(Locale.ROOT);
        return Character.toUpperCase(first.charAt(0)) + first.substring(1);
    }
}
