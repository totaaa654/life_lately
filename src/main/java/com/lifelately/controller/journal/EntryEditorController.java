package com.lifelately.controller.journal;

import com.lifelately.config.AppConfig;
import com.lifelately.model.Entry;
import com.lifelately.model.Mood;
import com.lifelately.navigation.NavigationManager;
import com.lifelately.navigation.View;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

import java.time.LocalDate;
import java.util.stream.Collectors;

public final class EntryEditorController {
    @FXML private Label pageTitle;
    @FXML private Label weekdayLabel;
    @FXML private Label fullDateLabel;
    @FXML private DatePicker datePicker;
    @FXML private ToggleGroup moodGroup;
    @FXML private TextField titleField;
    @FXML private TextArea contentArea;
    @FXML private TextField tagsField;
    @FXML private CheckBox favoriteBox;
    @FXML private Label errorLabel;
    @FXML private Label wordCountLabel;

    private final AppConfig config = AppConfig.getInstance();
    private java.util.List<Mood> moods;
    private Long entryId;

    @FXML
    private void initialize() {
        datePicker.setValue(LocalDate.now());
        updateDateHeading(LocalDate.now());
        datePicker.valueProperty().addListener((observable, oldValue, newValue) -> updateDateHeading(newValue));
        moods = config.moodService().getMoods();
        selectMood("Good");
        contentArea.textProperty().addListener((observable, oldValue, newValue) -> updateCount(newValue));
    }

    public void setEntry(Long id) {
        entryId = id;
        if (id == null) return;
        Entry entry = config.entryService().getEntry(id).orElseThrow();
        pageTitle.setText("Edit this moment");
        datePicker.setValue(entry.getEntryDate());
        selectMood(entry.getMood().name());
        titleField.setText(entry.getTitle());
        contentArea.setText(entry.getContent());
        favoriteBox.setSelected(entry.isFavorite());
        tagsField.setText(entry.getTags().stream().map(tag -> tag.name()).collect(Collectors.joining(", ")));
    }

    @FXML
    private void save() {
        try {
            Entry saved = config.entryService().save(entryId, titleField.getText(), contentArea.getText(),
                    selectedMood(), datePicker.getValue(), favoriteBox.isSelected(), tagsField.getText());
            NavigationManager.getInstance().openDetails(saved.getId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            errorLabel.setText(exception.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML private void cancel() {
        if (entryId == null) NavigationManager.getInstance().navigate(View.JOURNAL);
        else NavigationManager.getInstance().openDetails(entryId);
    }

    private void updateCount(String content) {
        int words = content == null || content.isBlank() ? 0 : content.trim().split("\\s+").length;
        wordCountLabel.setText(words + (words == 1 ? " word" : " words"));
    }

    public void selectMood(String moodName) {
        if (moodName == null || moodGroup == null) return;
        moodGroup.getToggles().stream()
                .filter(toggle -> moodName.equalsIgnoreCase(String.valueOf(toggle.getUserData())))
                .findFirst().ifPresent(moodGroup::selectToggle);
    }

    private Mood selectedMood() {
        Toggle selected = moodGroup.getSelectedToggle();
        if (selected == null) return null;
        String name = String.valueOf(selected.getUserData());
        return moods.stream().filter(mood -> mood.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private void updateDateHeading(LocalDate date) {
        if (date == null) return;
        weekdayLabel.setText(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL,
                java.util.Locale.getDefault()));
        fullDateLabel.setText(date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")));
    }
}
