package com.lifelately.controller.settings;

import com.lifelately.config.AppConfig;
import com.lifelately.model.AppSettings;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public final class SettingsController {
    @FXML private ComboBox<String> themeBox;
    @FXML private ComboBox<String> accentBox;
    @FXML private ComboBox<String> dateFormatBox;
    @FXML private ComboBox<String> firstDayBox;
    @FXML private Label statusLabel;

    private final AppConfig config = AppConfig.getInstance();

    @FXML
    private void initialize() {
        themeBox.getItems().setAll("Light", "Dark", "System");
        accentBox.getItems().setAll("Blue", "Yellow", "Green", "Lavender", "Coral");
        dateFormatBox.getItems().setAll("MMM d, yyyy", "MMMM d, yyyy", "dd/MM/yyyy", "yyyy-MM-dd");
        firstDayBox.getItems().setAll("Monday", "Sunday");
        AppSettings settings = config.isDatabaseReady() ? config.settingsService().getSettings() : AppSettings.defaults();
        themeBox.setValue(titleCase(settings.theme()));
        accentBox.setValue(titleCase(settings.accentColor()));
        dateFormatBox.setValue(settings.dateFormat());
        firstDayBox.setValue(titleCase(settings.firstDayOfWeek()));
    }

    @FXML
    private void applyPreview() {
        if (themeBox.getValue() == null || accentBox.getValue() == null) return;
        ThemeManager.apply(Theme.valueOf(themeBox.getValue().toUpperCase()), accentBox.getValue().toUpperCase());
    }

    @FXML
    private void save() {
        AppSettings settings = selectedSettings();
        ThemeManager.apply(Theme.valueOf(settings.theme()), settings.accentColor());
        if (!config.isDatabaseReady()) {
            showStatus("Theme preview applied. Start MySQL to persist settings.", true);
            return;
        }
        try {
            config.settingsService().save(settings);
            showStatus("Settings saved.", false);
        } catch (IllegalStateException exception) {
            showStatus(exception.getMessage(), true);
        }
    }

    private AppSettings selectedSettings() {
        return new AppSettings(themeBox.getValue().toUpperCase(), accentBox.getValue().toUpperCase(),
                dateFormatBox.getValue(), firstDayBox.getValue().toUpperCase());
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-label", "error-label");
        statusLabel.getStyleClass().add(error ? "error-label" : "success-label");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private String titleCase(String value) {
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
