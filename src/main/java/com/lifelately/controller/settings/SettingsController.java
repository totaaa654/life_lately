package com.lifelately.controller.settings;

import com.lifelately.App;
import com.lifelately.config.AppConfig;
import com.lifelately.model.AppSettings;
import com.lifelately.theme.Theme;
import com.lifelately.theme.ThemeManager;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;

public final class SettingsController {
    @FXML private ScrollPane settingsRoot;
    @FXML private ComboBox<String> themeBox;
    @FXML private ComboBox<String> accentBox;
    @FXML private ComboBox<String> dateFormatBox;
    @FXML private ComboBox<String> firstDayBox;
    @FXML private Label usernameValue;
    @FXML private TextField displayNameField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final AppConfig config = AppConfig.getInstance();
    private boolean initializing;

    @FXML
    private void initialize() {
        initializing = true;
        themeBox.getItems().setAll("Light", "Dark", "System");
        accentBox.getItems().setAll("Blue", "Yellow", "Green", "Lavender", "Coral");
        dateFormatBox.getItems().setAll("MMM d, yyyy", "MMMM d, yyyy", "dd/MM/yyyy", "yyyy-MM-dd");
        firstDayBox.getItems().setAll("Monday", "Sunday");
        AppSettings settings = config.isDatabaseReady() ? config.settingsService().getSettings() : AppSettings.defaults();
        String activeTheme = ThemeManager.isInitialized() ? ThemeManager.currentTheme().name() : settings.theme();
        String activeAccent = ThemeManager.isInitialized() ? ThemeManager.currentAccent() : settings.accentColor();
        themeBox.setValue(titleCase(activeTheme));
        accentBox.setValue(titleCase(activeAccent));
        dateFormatBox.setValue(settings.dateFormat());
        firstDayBox.setValue(titleCase(settings.firstDayOfWeek()));
        config.authService().currentUser().ifPresent(user -> {
            usernameValue.setText("@" + user.username());
            displayNameField.setText(user.displayName());
        });
        initializing = false;
    }

    @FXML
    private void applyPreview() {
        if (themeBox.getValue() == null || accentBox.getValue() == null) return;
        String theme = themeBox.getValue().toUpperCase();
        String accent = accentBox.getValue().toUpperCase();
        ThemeManager.apply(Theme.valueOf(theme), accent);
        if (initializing || !config.isDatabaseReady()) return;
        try {
            config.settingsService().saveAppearance(theme, accent);
            showStatus("Appearance saved automatically.", false);
        } catch (IllegalStateException exception) {
            showStatus(exception.getMessage(), true);
        }
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
            showStatus("Journal preferences saved.", false);
        } catch (IllegalStateException exception) {
            showStatus(exception.getMessage(), true);
        }
    }

    @FXML
    private void logout() {
        config.authService().logout();
        App.showLogin(settingsRoot.getScene());
    }

    @FXML
    private void saveDisplayName() {
        try {
            var user = config.authService().updateDisplayName(displayNameField.getText());
            displayNameField.setText(user.displayName());
            showStatus("Display name updated.", false);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showStatus(exception.getMessage(), true);
        }
    }

    @FXML
    private void changePassword() {
        try {
            config.authService().changePassword(currentPasswordField.getText(), newPasswordField.getText(),
                    confirmPasswordField.getText());
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            showStatus("Password changed successfully.", false);
        } catch (IllegalArgumentException | IllegalStateException exception) {
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
