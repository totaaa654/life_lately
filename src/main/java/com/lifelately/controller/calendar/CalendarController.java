package com.lifelately.controller.calendar;

import com.lifelately.config.AppConfig;
import com.lifelately.model.Entry;
import com.lifelately.navigation.NavigationManager;
import com.lifelately.util.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class CalendarController {
    @FXML private Label monthTitle;
    @FXML private GridPane calendarGrid;
    @FXML private Label selectedDateTitle;
    @FXML private VBox selectedEntries;
    @FXML private VBox unavailableState;
    @FXML private Label monthMemoriesLabel;
    @FXML private Label monthMoodLabel;
    @FXML private Label monthStreakLabel;

    private final AppConfig config = AppConfig.getInstance();
    private YearMonth displayedMonth = YearMonth.now();
    private List<Entry> entries = List.of();

    @FXML
    private void initialize() {
        if (!config.isDatabaseReady()) {
            unavailableState.setVisible(true);
            unavailableState.setManaged(true);
            return;
        }
        entries = config.entryService().getAllEntries();
        var insights = config.entryService().getInsights();
        monthMemoriesLabel.setText(insights.entriesThisMonth() + " memories");
        monthMoodLabel.setText(insights.mostCommonMood() + " days lately");
        monthStreakLabel.setText(insights.currentStreak() + " day streak");
        renderMonth();
        showDate(LocalDate.now());
    }

    @FXML private void previousMonth() { displayedMonth = displayedMonth.minusMonths(1); renderMonth(); }
    @FXML private void nextMonth() { displayedMonth = displayedMonth.plusMonths(1); renderMonth(); }
    @FXML private void today() { displayedMonth = YearMonth.now(); renderMonth(); showDate(LocalDate.now()); }

    private void renderMonth() {
        calendarGrid.getChildren().clear();
        monthTitle.setText(displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + displayedMonth.getYear());
        String[] headings = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (int column = 0; column < headings.length; column++) {
            Label heading = new Label(headings[column]);
            heading.getStyleClass().add("calendar-heading");
            calendarGrid.add(heading, column, 0);
        }
        Map<LocalDate, List<Entry>> byDate = entries.stream().collect(Collectors.groupingBy(Entry::getEntryDate));
        LocalDate first = displayedMonth.atDay(1);
        int offset = first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            LocalDate date = displayedMonth.atDay(day);
            int position = offset + day - 1;
            Button dayButton = new Button(Integer.toString(day));
            dayButton.getStyleClass().add("calendar-day");
            if (date.equals(LocalDate.now())) dayButton.getStyleClass().add("calendar-today");
            List<Entry> dateEntries = byDate.getOrDefault(date, List.of());
            if (!dateEntries.isEmpty()) {
                dayButton.getStyleClass().add("calendar-has-entry");
                dayButton.getStyleClass().add("calendar-mood-" + dateEntries.getFirst().getMood().name().toLowerCase());
                dayButton.setText(Integer.toString(day));
            }
            dayButton.setOnAction(event -> showDate(date));
            calendarGrid.add(dayButton, position % 7, position / 7 + 1);
        }
    }

    private void showDate(LocalDate date) {
        selectedDateTitle.setText(DateUtils.format(date, "EEEE, MMMM d"));
        selectedEntries.getChildren().clear();
        List<Entry> matching = entries.stream().filter(entry -> entry.getEntryDate().equals(date)).toList();
        if (matching.isEmpty()) {
            Label empty = new Label("No entry for this day.");
            empty.getStyleClass().add("muted-label");
            selectedEntries.getChildren().add(empty);
            return;
        }
        for (Entry entry : matching) {
            Button row = new Button(entry.getMood().icon() + "  " + entry.getTitle());
            row.getStyleClass().add("calendar-entry-row");
            row.setOnAction(event -> NavigationManager.getInstance().openDetails(entry.getId()));
            selectedEntries.getChildren().add(row);
        }
    }
}
