package com.lifelately.controller.journal;

import com.lifelately.config.AppConfig;
import com.lifelately.model.Entry;
import com.lifelately.navigation.NavigationManager;
import com.lifelately.navigation.View;
import com.lifelately.util.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;

public final class EntryDetailsController {
    @FXML private Label dateLabel;
    @FXML private Label moodLabel;
    @FXML private Label favoriteLabel;
    @FXML private Label titleLabel;
    @FXML private TextArea contentArea;
    @FXML private FlowPane tagsPane;

    private final AppConfig config = AppConfig.getInstance();
    private long entryId;

    public void setEntry(long id) {
        entryId = id;
        Entry entry = config.entryService().getEntry(id).orElseThrow();
        dateLabel.setText(DateUtils.format(entry.getEntryDate(), "EEEE, MMMM d, yyyy"));
        moodLabel.setText(entry.getMood().name());
        favoriteLabel.setText(entry.isFavorite() ? "Favorite" : "");
        favoriteLabel.setVisible(entry.isFavorite());
        favoriteLabel.setManaged(entry.isFavorite());
        titleLabel.setText(entry.getTitle());
        contentArea.setText(entry.getContent());
        tagsPane.getChildren().clear();
        entry.getTags().forEach(tag -> {
            Label chip = new Label("#" + tag.name());
            chip.getStyleClass().add("tag-chip");
            tagsPane.getChildren().add(chip);
        });
    }

    @FXML private void edit() { NavigationManager.getInstance().openEditor(entryId); }
    @FXML private void back() { NavigationManager.getInstance().navigate(View.JOURNAL); }

    @FXML
    private void delete() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Move this entry out of your journal? It is soft-deleted and can be restored later.",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText("Delete this entry?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            config.entryService().delete(entryId);
            NavigationManager.getInstance().navigate(View.JOURNAL);
        }
    }
}
