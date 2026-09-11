package com.lifelately.controller.journal;

import com.lifelately.config.AppConfig;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.model.Tag;
import com.lifelately.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public final class JournalController {
    @FXML private TextField searchField;
    @FXML private ComboBox<Mood> moodFilter;
    @FXML private ComboBox<Tag> tagFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private CheckBox favoritesFilter;
    @FXML private DatePicker dateFilter;
    @FXML private VBox entriesContainer;
    @FXML private VBox unavailableState;
    @FXML private Label resultCount;

    private final AppConfig config = AppConfig.getInstance();

    @FXML
    private void initialize() {
        sortBox.getItems().setAll("Newest", "Oldest", "Recently edited");
        sortBox.setValue("Newest");
        if (!config.isDatabaseReady()) {
            unavailableState.setVisible(true);
            unavailableState.setManaged(true);
            return;
        }
        moodFilter.getItems().setAll(config.moodService().getMoods());
        tagFilter.getItems().setAll(config.tagService().getTags());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
        refresh();
    }

    @FXML private void refresh() {
        if (!config.isDatabaseReady()) return;
        String sort = switch (sortBox.getValue()) {
            case "Oldest" -> "OLDEST";
            case "Recently edited" -> "EDITED";
            default -> "NEWEST";
        };
        List<Entry> entries = config.entryService().search(searchField.getText(), moodFilter.getValue(),
                tagFilter.getValue(), favoritesFilter.isSelected(), sort);
        if (dateFilter.getValue() != null) {
            entries = entries.stream().filter(entry -> entry.getEntryDate().equals(dateFilter.getValue())).toList();
        }
        entriesContainer.getChildren().clear();
        for (Entry entry : entries) entriesContainer.getChildren().add(loadCard(entry));
        if (entries.isEmpty()) entriesContainer.getChildren().add(emptyMessage());
        resultCount.setText(entries.size() + (entries.size() == 1 ? " memory" : " memories"));
    }

    @FXML private void clearFilters() {
        searchField.clear();
        moodFilter.setValue(null);
        tagFilter.setValue(null);
        dateFilter.setValue(null);
        favoritesFilter.setSelected(false);
        sortBox.setValue("Newest");
        refresh();
    }

    @FXML private void newEntry() {
        if (config.isDatabaseReady()) NavigationManager.getInstance().openEditor(null);
    }

    private Parent loadCard(Entry entry) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lifelately/fxml/components/entry-card.fxml"));
            Parent card = loader.load();
            ((EntryCardController) loader.getController()).setEntry(entry);
            return card;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not display a journal entry.", exception);
        }
    }

    private VBox emptyMessage() {
        boolean filtering = !searchField.getText().isBlank() || moodFilter.getValue() != null
                || tagFilter.getValue() != null || dateFilter.getValue() != null || favoritesFilter.isSelected();
        String imageName = filtering ? "bear-search.png" : "bear-empty.png";
        ImageView bear = new ImageView(new Image(getClass().getResourceAsStream(
                "/com/lifelately/images/bear/" + imageName)));
        bear.setFitWidth(138);
        bear.setFitHeight(138);
        bear.setPreserveRatio(true);
        Label title = new Label(filtering ? "Hmm… I couldn’t find that one." : "It’s a little quiet here.");
        title.getStyleClass().add("empty-title");
        Label copy = new Label(filtering
                ? "Try a different word or clear a filter."
                : "Your memories will show up here once you start writing.");
        copy.getStyleClass().add("muted-label");
        Button action = new Button(filtering ? "Clear filters" : "Write your first memory");
        action.getStyleClass().add("secondary-button");
        action.setOnAction(event -> { if (filtering) clearFilters(); else newEntry(); });
        VBox empty = new VBox(8, bear, title, copy, action);
        empty.getStyleClass().add("empty-state-large");
        return empty;
    }
}
