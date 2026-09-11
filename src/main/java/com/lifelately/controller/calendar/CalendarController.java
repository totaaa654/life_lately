package com.lifelately.controller.calendar;

import com.lifelately.config.AppConfig;
import com.lifelately.model.Entry;
import com.lifelately.navigation.NavigationManager;
import com.lifelately.util.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    @FXML private Label selectedDateCount;
    @FXML private VBox selectedEntries;
    @FXML private VBox unavailableState;
    @FXML private Label monthMemoriesLabel;
    @FXML private Label monthMoodLabel;
    @FXML private Label monthStreakLabel;

    private final AppConfig config = AppConfig.getInstance();
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();
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
        monthMemoriesLabel.setText(insights.entriesThisMonth() +
                (insights.entriesThisMonth() == 1 ? " memory" : " memories"));
        monthMoodLabel.setText(insights.mostCommonMood() + " most often");
        monthStreakLabel.setText(insights.currentStreak() + " day streak");
        renderMonth();
        showDate(LocalDate.now());
    }

    @FXML private void previousMonth() { showMonth(displayedMonth.minusMonths(1)); }
    @FXML private void nextMonth() { showMonth(displayedMonth.plusMonths(1)); }
    @FXML private void today() { showMonth(YearMonth.now()); showDate(LocalDate.now()); updateSelectedDay(); }

    @FXML
    private void addForSelectedDate() {
        NavigationManager.getInstance().openEditor(null, null, selectedDate);
    }

    private void renderMonth() {
        calendarGrid.getChildren().clear();
        if (calendarGrid.getColumnConstraints().isEmpty()) {
            for (int column = 0; column < 7; column++) {
                ColumnConstraints constraints = new ColumnConstraints();
                constraints.setPercentWidth(100.0 / 7.0);
                constraints.setHgrow(Priority.ALWAYS);
                constraints.setFillWidth(true);
                calendarGrid.getColumnConstraints().add(constraints);
            }
        }
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
            dayButton.setUserData(date);
            dayButton.getStyleClass().add("calendar-day");
            if (date.equals(LocalDate.now())) dayButton.getStyleClass().add("calendar-today");
            if (date.equals(selectedDate)) dayButton.getStyleClass().add("calendar-selected");
            List<Entry> dateEntries = byDate.getOrDefault(date, List.of());
            if (!dateEntries.isEmpty()) {
                dayButton.getStyleClass().add("calendar-has-entry");
                dayButton.getStyleClass().add("calendar-mood-" + dateEntries.getFirst().getMood().name().toLowerCase());
                dayButton.setText(Integer.toString(day));
            }
            dayButton.setOnAction(event -> {
                showDate(date);
                updateSelectedDay();
            });
            calendarGrid.add(dayButton, position % 7, position / 7 + 1);
        }
    }

    private void showMonth(YearMonth month) {
        displayedMonth = month;
        selectedDate = month.atDay(1);
        renderMonth();
        showDate(selectedDate);
    }

    private void updateSelectedDay() {
        calendarGrid.getChildren().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .forEach(button -> {
                    button.getStyleClass().remove("calendar-selected");
                    if (selectedDate.equals(button.getUserData())) {
                        button.getStyleClass().add("calendar-selected");
                    }
                });
    }

    private void showDate(LocalDate date) {
        selectedDate = date;
        selectedDateTitle.setText(DateUtils.format(date, "EEEE, MMMM d"));
        selectedEntries.getChildren().clear();
        List<Entry> matching = entries.stream().filter(entry -> entry.getEntryDate().equals(date)).toList();
        selectedDateCount.setText(matching.size() + (matching.size() == 1 ? " memory" : " memories"));
        if (matching.isEmpty()) {
            VBox empty = new VBox(3);
            Label title = new Label("A clear page");
            title.getStyleClass().add("calendar-empty-title");
            Label copy = new Label("Nothing written for this date yet.");
            copy.setWrapText(true);
            copy.getStyleClass().add("muted-label");
            empty.getChildren().addAll(title, copy);
            empty.getStyleClass().add("calendar-empty-state");
            selectedEntries.getChildren().add(empty);
            return;
        }
        for (Entry entry : matching) {
            Region marker = new Region();
            marker.getStyleClass().addAll("calendar-entry-marker",
                    "dot-" + entry.getMood().name().toLowerCase());

            Label mood = new Label(entry.getMood().name());
            mood.getStyleClass().add("calendar-entry-mood");
            HBox metadata = new HBox(6, marker, mood);
            metadata.getStyleClass().add("calendar-entry-meta");

            Label title = new Label(entry.getTitle());
            title.setWrapText(true);
            title.setMaxWidth(246);
            title.getStyleClass().add("calendar-entry-title");
            String excerpt = entry.getContent().replaceAll("\\s+", " ").strip();
            if (excerpt.length() > 92) excerpt = excerpt.substring(0, 92) + "...";
            Label preview = new Label(excerpt.isBlank() ? "No written details." : excerpt);
            preview.setWrapText(true);
            preview.setMaxWidth(246);
            preview.getStyleClass().add("calendar-entry-preview");

            VBox overview = new VBox(4, metadata, title, preview);
            overview.setFillWidth(true);
            overview.setPrefWidth(246);
            Button row = new Button();
            row.setGraphic(overview);
            row.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            row.getStyleClass().add("calendar-entry-row");
            row.setOnAction(event -> NavigationManager.getInstance().openDetails(entry.getId()));
            selectedEntries.getChildren().add(row);
        }
    }
}
