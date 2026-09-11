package com.lifelately.controller.insights;

import com.lifelately.config.AppConfig;
import com.lifelately.service.EntryService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class InsightsController {
    @FXML private Label totalValue;
    @FXML private Label monthValue;
    @FXML private Label currentStreakValue;
    @FXML private Label longestStreakValue;
    @FXML private Label commonMoodValue;
    @FXML private VBox moodCounts;
    @FXML private HBox topTags;
    @FXML private VBox unavailableState;
    @FXML private Label observationLabel;

    @FXML
    private void initialize() {
        AppConfig config = AppConfig.getInstance();
        if (!config.isDatabaseReady()) {
            unavailableState.setVisible(true);
            unavailableState.setManaged(true);
            return;
        }
        EntryService.Insights insights = config.entryService().getInsights();
        totalValue.setText(Integer.toString(insights.totalEntries()));
        monthValue.setText(Integer.toString(insights.entriesThisMonth()));
        currentStreakValue.setText(Integer.toString(insights.currentStreak()));
        longestStreakValue.setText(Integer.toString(insights.longestStreak()));
        commonMoodValue.setText(insights.mostCommonMood());
        long maximum = insights.moodCounts().values().stream().mapToLong(Long::longValue).max().orElse(1);
        insights.moodCounts().entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .forEach(entry -> {
            Label moodName = new Label(entry.getKey());
            moodName.getStyleClass().add("insight-mood-name");
            ProgressBar bar = new ProgressBar((double) entry.getValue() / maximum);
            bar.getStyleClass().add("mood-bar-" + entry.getKey().toLowerCase());
            HBox.setHgrow(bar, javafx.scene.layout.Priority.ALWAYS);
            Label count = new Label(Long.toString(entry.getValue()));
            count.getStyleClass().add("insight-count");
            HBox row = new HBox(12, moodName, bar, count);
            row.getStyleClass().add("insight-row");
            moodCounts.getChildren().add(row);
        });
        if (insights.moodCounts().isEmpty()) moodCounts.getChildren().add(new Label("No mood data yet."));
        insights.topTags().forEach(tag -> {
            Label chip = new Label("#" + tag);
            chip.getStyleClass().add("tag-chip");
            topTags.getChildren().add(chip);
        });
        if (insights.topTags().isEmpty()) topTags.getChildren().add(new Label("No tags yet."));
        observationLabel.setText(insights.totalEntries() == 0
                ? "Once you’ve written a few memories, I’ll help you notice the little patterns."
                : "Looks like “" + insights.mostCommonMood()
                    + "” has shown up most often lately. Every feeling belongs here.");
    }
}
