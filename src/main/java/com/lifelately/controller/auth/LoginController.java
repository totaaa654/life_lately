package com.lifelately.controller.auth;

import com.lifelately.App;
import com.lifelately.config.AppConfig;
import com.lifelately.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class LoginController {
    @FXML private StackPane loginRoot;
    @FXML private Label formEyebrow;
    @FXML private Label formTitle;
    @FXML private Label formSubtitle;
    @FXML private VBox displayNameGroup;
    @FXML private VBox confirmationGroup;
    @FXML private TextField displayNameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmationField;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;

    private final AppConfig config = AppConfig.getInstance();
    private boolean firstAccount;

    @FXML
    private void initialize() {
        if (!config.isDatabaseReady()) {
            formEyebrow.setText("DATABASE CONNECTION");
            formTitle.setText("MySQL is not available");
            formSubtitle.setText("Start MySQL in Laragon, then restart Life Lately.");
            submitButton.setDisable(true);
            showError(config.databaseMessage());
            return;
        }

        firstAccount = config.authService().requiresFirstAccount();
        displayNameGroup.setVisible(firstAccount);
        displayNameGroup.setManaged(firstAccount);
        confirmationGroup.setVisible(firstAccount);
        confirmationGroup.setManaged(firstAccount);
        if (firstAccount) {
            formEyebrow.setText("FIRST TIME SETUP");
            formTitle.setText("Create your private account");
            formSubtitle.setText("Your journal stays connected to this local MySQL database.");
            submitButton.setText("Create account");
        }
    }

    @FXML
    private void submit() {
        clearStatus();
        try {
            AuthService auth = config.authService();
            if (firstAccount) {
                auth.createFirstAccount(usernameField.getText(), displayNameField.getText(),
                        passwordField.getText(), confirmationField.getText());
            } else {
                auth.login(usernameField.getText(), passwordField.getText());
            }
            App.showMain(loginRoot.getScene());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void showError(String message) {
        statusLabel.setText(message == null || message.isBlank() ? "Unable to continue." : message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
