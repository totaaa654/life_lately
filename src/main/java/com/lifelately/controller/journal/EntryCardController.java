package com.lifelately.controller.journal;

import com.lifelately.model.Entry;
import com.lifelately.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

public final class EntryCardController {
    @FXML private HBox cardRoot;
    @FXML private Label monthLabel;
    @FXML private Label dayLabel;
    @FXML private Label favoriteLabel;
    @FXML private Label moodLabel;
    @FXML private Label titleLabel;
    @FXML private Label previewLabel;
    @FXML private FlowPane tagPane;

    private long entryId;

    public void setEntry(Entry entry) {
        entryId = entry.getId();
        monthLabel.setText(entry.getEntryDate().getMonth().toString().substring(0, 3));
        dayLabel.setText(Integer.toString(entry.getEntryDate().getDayOfMonth()));
        favoriteLabel.setText(entry.isFavorite() ? "Saved" : "");
        moodLabel.setText(entry.getMood().name());
        moodLabel.getStyleClass().add("mood-" + entry.getMood().name().toLowerCase());
        cardRoot.getStyleClass().add("journal-card-" + entry.getMood().name().toLowerCase());
        titleLabel.setText(entry.getTitle());
        String plain = entry.getContent().replaceAll("\\s+", " ").trim();
        previewLabel.setText(plain.length() > 150 ? plain.substring(0, 150) + "…" : plain);
        tagPane.getChildren().clear();
        entry.getTags().forEach(tag -> {
            Label chip = new Label("#" + tag.name());
            chip.getStyleClass().add("tag-chip");
            tagPane.getChildren().add(chip);
        });
        cardRoot.setOnMouseClicked(event -> NavigationManager.getInstance().openDetails(entryId));
    }
}
