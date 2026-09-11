package com.lifelately.controller;

import com.lifelately.config.AppConfig;
import com.lifelately.navigation.NavigationManager;
import com.lifelately.navigation.View;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.List;

public final class MainController {
    @FXML private StackPane contentHost;
    @FXML private Button homeButton;
    @FXML private Button journalButton;
    @FXML private Button calendarButton;
    @FXML private Button insightsButton;
    @FXML private Button settingsButton;
    @FXML private Label databaseStatus;

    private NavigationManager navigation;

    @FXML
    private void initialize() {
        navigation = new NavigationManager(contentHost, this::updateActiveButton);
        AppConfig config = AppConfig.getInstance();
        databaseStatus.setText(config.isDatabaseReady()
                ? config.authService().currentUser()
                    .map(user -> "Signed in as " + user.username())
                    .orElse("Sign-in required")
                : "MySQL offline");
        databaseStatus.getStyleClass().add(config.isDatabaseReady() ? "status-online" : "status-offline");
        navigation.navigate(View.HOME);
    }

    @FXML private void showHome() { navigation.navigate(View.HOME); }
    @FXML private void showJournal() { navigation.navigate(View.JOURNAL); }
    @FXML private void showCalendar() { navigation.navigate(View.CALENDAR); }
    @FXML private void showInsights() { navigation.navigate(View.INSIGHTS); }
    @FXML private void showSettings() { navigation.navigate(View.SETTINGS); }

    private void updateActiveButton(View view) {
        List<Button> buttons = List.of(homeButton, journalButton, calendarButton, insightsButton, settingsButton);
        buttons.forEach(button -> button.getStyleClass().remove("sidebar-item-active"));
        Button active = switch (view) {
            case HOME -> homeButton;
            case JOURNAL, ENTRY_EDITOR, ENTRY_DETAILS -> journalButton;
            case CALENDAR -> calendarButton;
            case INSIGHTS -> insightsButton;
            case SETTINGS -> settingsButton;
        };
        active.getStyleClass().add("sidebar-item-active");
    }
}
